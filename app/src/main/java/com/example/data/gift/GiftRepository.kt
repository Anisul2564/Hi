package com.example.data.gift

import com.example.model.FirestoreGift
import com.example.model.FirestoreGiftTransaction
import com.example.model.GiftTransactionStatus
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
 * Production-ready GiftRepository for managing gift catalog queries and
 * submitting backend-ready gift transaction intents.
 *
 * Direct balance modification and COMPLETED status setting are forbidden client-side.
 */
class GiftRepository {
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    /**
     * Streams enabled gifts from /gifts in Firestore.
     * Falls back to standard BD PARTY gift catalog if collection is empty.
     */
    fun observeGiftCatalog(): Flow<List<FirestoreGift>> = callbackFlow {
        val ref = db.collection("gifts")
            .whereEqualTo("enabled", true)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(defaultGifts())
                return@addSnapshotListener
            }
            val gifts = snapshot.documents.mapNotNull { it.toObject(FirestoreGift::class.java) }
                .sortedBy { it.sortOrder }

            if (gifts.isEmpty()) {
                trySend(defaultGifts())
            } else {
                trySend(gifts)
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Submits a gift send request.
     * Verifies:
     * - Sender is the authenticated user
     * - Receiver is not the sender
     * - Receiver UID is valid
     * - Quantity is positive (1..9999)
     * - Creates transaction with status = PENDING
     *
     * In accordance with security instructions, client does NOT deduct coins or mark COMPLETED.
     */
    suspend fun requestSendGift(
        roomId: String,
        receiverUid: String,
        receiverBdpId: String,
        receiverUsername: String,
        gift: FirestoreGift,
        quantity: Int,
        senderBdpId: String,
        senderUsername: String
    ): Result<FirestoreGiftTransaction> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not signed in"))

        if (receiverUid.isBlank() || receiverUid == currentUser.uid) {
            return Result.failure(IllegalArgumentException("Cannot send a gift to yourself"))
        }

        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than 0"))
        }

        if (quantity > 9999) {
            return Result.failure(IllegalArgumentException("Maximum gift quantity is 9,999"))
        }

        val totalCoins = gift.coinPrice * quantity
        val transactionId = UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val transaction = FirestoreGiftTransaction(
            transactionId = transactionId,
            senderUid = currentUser.uid,
            senderBdpId = senderBdpId,
            senderUsername = senderUsername,
            receiverUid = receiverUid,
            receiverBdpId = receiverBdpId,
            receiverUsername = receiverUsername,
            giftId = gift.giftId,
            giftName = gift.name,
            coinPrice = gift.coinPrice,
            quantity = quantity,
            totalCoins = totalCoins,
            roomId = roomId,
            createdAt = now,
            status = GiftTransactionStatus.PENDING.name
        )

        return try {
            // Write PENDING transaction to Firestore
            db.collection("giftTransactions").document(transactionId)
                .set(transaction.toMap())
                .await()
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates gift sending parameters locally with safe limits and strict anti-self gifting check.
     * Returns an error message if invalid, or null if valid.
     */
    fun validateGiftRequest(
        receiverUid: String,
        currentUserUid: String,
        quantity: Int
    ): String? {
        if (currentUserUid.isBlank()) {
            return "User is not signed in"
        }
        if (receiverUid.isBlank()) {
            return "Please select a valid stage member"
        }
        if (receiverUid == currentUserUid) {
            return "Cannot send a gift to yourself"
        }
        if (quantity <= 0) {
            return "Quantity must be greater than 0"
        }
        if (quantity > 9999) {
            return "Maximum gift quantity is 9,999"
        }
        return null
    }

    /**
     * Observes gift transactions sent by the user.
     */
    fun observeSentGifts(uid: String): Flow<List<FirestoreGiftTransaction>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.collection("giftTransactions")
            .whereEqualTo("senderUid", uid)

        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { it.toObject(FirestoreGiftTransaction::class.java) }
                .sortedByDescending { it.createdAt }
            trySend(list)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Observes gift transactions received by the user.
     */
    fun observeReceivedGifts(uid: String): Flow<List<FirestoreGiftTransaction>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.collection("giftTransactions")
            .whereEqualTo("receiverUid", uid)

        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { it.toObject(FirestoreGiftTransaction::class.java) }
                .sortedByDescending { it.createdAt }
            trySend(list)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Observes gift transactions involving the user as sender.
     */
    fun observeUserTransactions(uid: String): Flow<List<FirestoreGiftTransaction>> = observeSentGifts(uid)

    /**
     * Default BD PARTY gifts fallback catalog.
     */
    fun defaultGifts(): List<FirestoreGift> = listOf(
        FirestoreGift("g_rose", "Rose", "A romantic crimson rose", "🌹", "", 10L, true, 1),
        FirestoreGift("g_coffee", "Warm Coffee", "Fresh brew for the voice stage", "☕", "", 50L, true, 2),
        FirestoreGift("g_mic", "Golden Mic", "Reward outstanding singing & speech", "🎙️", "", 100L, true, 3),
        FirestoreGift("g_heart", "Glowing Heart", "Send love to the stage creator", "💖", "", 200L, true, 4),
        FirestoreGift("g_fire", "Party Fire", "Ignite stage energy & crowd cheer", "🔥", "", 500L, true, 5),
        FirestoreGift("g_crown", "Crown of Honor", "Prestige crown for top speakers", "👑", "", 1000L, true, 6),
        FirestoreGift("g_rocket", "Space Rocket", "Blast off with high-flying hype", "🚀", "", 2000L, true, 7),
        FirestoreGift("g_car", "Sports Car", "Luxury supercar entrance", "🏎️", "", 5000L, true, 8),
        FirestoreGift("g_diamond", "BD Diamond", "Pure luxury diamond burst", "💎", "", 10000L, true, 9),
        FirestoreGift("g_castle", "Royal Castle", "Grand palace display for room royalty", "🏰", "", 20000L, true, 10)
    )
}
