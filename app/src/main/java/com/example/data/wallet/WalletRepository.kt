package com.example.data.wallet

import com.example.model.FirestoreCoinPackage
import com.example.model.FirestorePaymentRequest
import com.example.model.FirestoreWallet
import com.example.model.PaymentRequestStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Production-ready WalletRepository for managing user wallet queries and
 * creating manual recharge requests (bKash/Nagad/Rocket).
 *
 * Direct balance manipulation is strictly forbidden client-side.
 */
class WalletRepository {
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    /**
     * Real-time stream of the authenticated user's wallet document.
     * Emits null if no user is signed in or wallet is initializing.
     */
    fun observeWallet(uid: String): Flow<FirestoreWallet?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val walletRef = db.collection("wallets").document(uid)
        val registration: ListenerRegistration = walletRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val wallet = snapshot.toObject(FirestoreWallet::class.java)
                trySend(wallet)
            } else {
                trySend(null)
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Initializes wallet with coinBalance = 0 if not already created.
     * Guaranteed to use the authenticated Firebase user UID.
     */
    suspend fun getOrCreateWallet(uid: String, bdpId: String): Result<FirestoreWallet> {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.uid != uid) {
            return Result.failure(SecurityException("Unauthorized access to wallet"))
        }

        val walletRef = db.collection("wallets").document(uid)
        return try {
            val snapshot = walletRef.get().await()
            if (snapshot.exists()) {
                val wallet = snapshot.toObject(FirestoreWallet::class.java)
                    ?: FirestoreWallet(uid = uid, bdpId = bdpId, coinBalance = 0L)
                Result.success(wallet)
            } else {
                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val newWallet = FirestoreWallet(
                    uid = uid,
                    bdpId = bdpId,
                    coinBalance = 0L,
                    totalPurchasedCoins = 0L,
                    totalGiftedCoins = 0L,
                    totalReceivedCoins = 0L,
                    updatedAt = now
                )
                walletRef.set(newWallet.toMap()).await()
                Result.success(newWallet)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of enabled coin packages.
     */
    fun observeCoinPackages(): Flow<List<FirestoreCoinPackage>> = callbackFlow {
        val ref = db.collection("coinPackages")
            .whereEqualTo("enabled", true)

        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(defaultCoinPackages())
                return@addSnapshotListener
            }
            val packages = snapshot.documents.mapNotNull { it.toObject(FirestoreCoinPackage::class.java) }
                .sortedBy { it.sortOrder }

            if (packages.isEmpty()) {
                trySend(defaultCoinPackages())
            } else {
                trySend(packages)
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Submits a manual payment verification request for bKash, Nagad, or Rocket.
     * The request status is set to PENDING. No balance is credited automatically.
     */
    suspend fun submitPaymentRequest(
        paymentMethod: String,
        packageId: String,
        coinAmount: Long,
        amountLabel: String,
        senderNumber: String,
        transactionId: String,
        userBdpId: String,
        username: String
    ): Result<String> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User not signed in"))

        if (paymentMethod.isBlank() || !listOf("bKash", "Nagad", "Rocket").contains(paymentMethod)) {
            return Result.failure(IllegalArgumentException("Please choose a valid payment method (bKash, Nagad, Rocket)"))
        }

        if (senderNumber.trim().length < 11) {
            return Result.failure(IllegalArgumentException("Please enter a valid 11-digit mobile number"))
        }

        if (transactionId.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Please enter a valid Transaction ID (TrxID)"))
        }

        val requestId = UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val request = FirestorePaymentRequest(
            requestId = requestId,
            uid = currentUser.uid,
            bdpId = userBdpId,
            username = username,
            paymentMethod = paymentMethod,
            packageId = packageId,
            coinAmount = coinAmount,
            amount = amountLabel,
            senderNumber = senderNumber.trim(),
            transactionId = transactionId.trim().uppercase(),
            screenshotUrl = "",
            status = PaymentRequestStatus.PENDING.name,
            createdAt = now,
            reviewedAt = null,
            reviewedBy = null,
            reviewNote = null
        )

        return try {
            db.collection("paymentRequests").document(requestId)
                .set(request.toMap())
                .await()
            Result.success(requestId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streams the user's manual payment requests.
     */
    fun observeUserPaymentRequests(uid: String): Flow<List<FirestorePaymentRequest>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.collection("paymentRequests")
            .whereEqualTo("uid", uid)

        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val requests = snapshot.documents.mapNotNull { it.toObject(FirestorePaymentRequest::class.java) }
                .sortedByDescending { it.createdAt }
            trySend(requests)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Streams all payment requests for administrative review.
     */
    fun observeAllPaymentRequests(): Flow<List<FirestorePaymentRequest>> = callbackFlow {
        val ref = db.collection("paymentRequests")

        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val requests = snapshot.documents.mapNotNull { it.toObject(FirestorePaymentRequest::class.java) }
                .sortedByDescending { it.createdAt }
            trySend(requests)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Authorized administrative review of a manual payment request.
     * Updates status to APPROVED or REJECTED with reviewer identity and timestamp.
     */
    suspend fun reviewPaymentRequest(
        requestId: String,
        newStatus: String,
        reviewerBdpId: String,
        reviewerRole: String,
        reviewNote: String
    ): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Admin authentication required"))

        if (newStatus !in listOf("APPROVED", "REJECTED")) {
            return Result.failure(IllegalArgumentException("Status must be APPROVED or REJECTED"))
        }

        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        return try {
            val updates = mapOf(
                "status" to newStatus,
                "reviewedAt" to now,
                "reviewedBy" to "$reviewerBdpId ($reviewerRole)",
                "reviewNote" to reviewNote.trim()
            )
            db.collection("paymentRequests").document(requestId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Built-in fallback coin packages for display if Firestore collection is not yet populated.
     */
    fun defaultCoinPackages(): List<FirestoreCoinPackage> = listOf(
        FirestoreCoinPackage("pkg_1", "Starter Pack", 10000L, "৳120", 1000L, true, 1),
        FirestoreCoinPackage("pkg_2", "Popular Bundle", 50000L, "৳600", 8000L, true, 2),
        FirestoreCoinPackage("pkg_3", "Stage Booster", 120000L, "৳1,400", 25000L, true, 3),
        FirestoreCoinPackage("pkg_4", "Party VIP Pack", 300000L, "৳3,500", 75000L, true, 4),
        FirestoreCoinPackage("pkg_5", "SVIP Crown Pack", 650000L, "৳7,000", 200000L, true, 5),
        FirestoreCoinPackage("pkg_6", "Royal Stage Master", 1500000L, "৳15,000", 600000L, true, 6)
    )
}
