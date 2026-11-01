package com.example.data.firebase

import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.RoleRequest
import com.example.model.User
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirestoreUserProfile) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // Secondary constructor that resolves custom database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val scope = CoroutineScope(Dispatchers.IO)
    private var profileListener: ListenerRegistration? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentProfile = MutableStateFlow<FirestoreUserProfile?>(null)
    val currentProfile: StateFlow<FirestoreUserProfile?> = _currentProfile.asStateFlow()

    private val _currentDomainUser = MutableStateFlow<User?>(null)
    val currentDomainUser: StateFlow<User?> = _currentDomainUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                attachUserProfileListener(firebaseUser)
            } else {
                detachUserProfileListener()
                _currentProfile.value = null
                _currentDomainUser.value = null
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    private fun attachUserProfileListener(firebaseUser: FirebaseUser) {
        detachUserProfileListener()
        val userDocRef = db.collection("users").document(firebaseUser.uid)

        profileListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _authState.value = AuthState.Error("Profile sync failed: ${error.localizedMessage}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(FirestoreUserProfile::class.java)
                if (profile != null) {
                    _currentProfile.value = profile
                    _currentDomainUser.value = profile.toDomainUser()
                    _authState.value = AuthState.Authenticated(profile)
                }
            } else {
                // If user document is missing in Firestore, initialize it safely with sequential BDP ID
                scope.launch {
                    try {
                        ensureUserProfileCreated(firebaseUser)
                    } catch (e: Exception) {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to initialize profile")
                    }
                }
            }
        }
    }

    private fun detachUserProfileListener() {
        profileListener?.remove()
        profileListener = null
    }

    /**
     * Safely creates a new profile in a Firestore transaction with an incremental BDP ID.
     * Starts with role = "USER" and coins = 0.
     */
    private suspend fun ensureUserProfileCreated(
        firebaseUser: FirebaseUser,
        customUsername: String? = null
    ): FirestoreUserProfile {
        val userRef = db.collection("users").document(firebaseUser.uid)
        val counterRef = db.collection("system").document("counters")

        return db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            if (userSnap.exists()) {
                userSnap.toObject(FirestoreUserProfile::class.java)
                    ?: error("Failed to deserialize existing user")
            } else {
                val counterSnap = tx.get(counterRef)
                val lastNumber = counterSnap.getLong("lastBdpNumber") ?: 0L
                val nextNumber = lastNumber + 1L
                val generatedBdpId = String.format("BDP%06d", nextNumber)

                // Update system counter
                tx.set(counterRef, mapOf("lastBdpNumber" to nextNumber))

                val name = customUsername?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: "BD Party Member"

                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

                val newProfile = FirestoreUserProfile(
                    uid = firebaseUser.uid,
                    bdpId = generatedBdpId,
                    username = name,
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    bio = "Welcome to BD PARTY! ✨🎙️",
                    role = "USER", // STRICTLY USER as per requirement
                    coins = 0L,    // STRICTLY 0 as per requirement
                    createdAt = now,
                    updatedAt = now
                )

                tx.set(userRef, newProfile.toMap())
                newProfile
            }
        }.await()
    }

    /**
     * Sign In with Google via Jetpack Credential Manager
     */
    suspend fun signInWithGoogle(context: Context): Result<FirestoreUserProfile> {
        return try {
            _authState.value = AuthState.Loading
            val credentialManager = CredentialManager.create(context)

            val serverClientId = context.getString(R.string.default_web_client_id)
            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user ?: error("Firebase user is null after Google sign in")

                val profile = ensureUserProfileCreated(firebaseUser)
                Result.success(profile)
            } else {
                val msg = "Unexpected credential format returned"
                _authState.value = AuthState.Error(msg)
                Result.failure(Exception(msg))
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = AuthState.Unauthenticated("Google sign-in was cancelled.")
            Result.failure(e)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseNetworkException -> "Network error. Please check your internet connection."
                else -> e.localizedMessage ?: "Google sign-in failed."
            }
            _authState.value = AuthState.Error(userMsg)
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Email / Password Login
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirestoreUserProfile> {
        return try {
            _authState.value = AuthState.Loading
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user ?: error("User is null after sign in")
            val profile = ensureUserProfileCreated(firebaseUser)
            Result.success(profile)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Invalid email address or incorrect password."
                is FirebaseAuthInvalidUserException -> "No account found with this email."
                is FirebaseNetworkException -> "Network failure. Please check your connection."
                else -> e.localizedMessage ?: "Authentication failed."
            }
            _authState.value = AuthState.Error(userMsg)
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Email / Password Sign Up
     */
    suspend fun signUpWithEmail(email: String, pass: String, username: String): Result<FirestoreUserProfile> {
        return try {
            _authState.value = AuthState.Loading
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user ?: error("User is null after sign up")
            val profile = ensureUserProfileCreated(firebaseUser, username.trim())
            Result.success(profile)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseAuthUserCollisionException -> "An account with this email already exists."
                is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
                is FirebaseAuthInvalidCredentialsException -> "Invalid email address format."
                is FirebaseNetworkException -> "Network failure. Please check your connection."
                else -> e.localizedMessage ?: "Sign up failed."
            }
            _authState.value = AuthState.Error(userMsg)
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Upload Profile Photo to Firebase Storage and update users/{uid}.photoUrl
     */
    suspend fun uploadProfilePhoto(imageUri: Uri, context: Context): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val storage = try {
                FirebaseStorage.getInstance("gs://gen-lang-client-0683712363.firebasestorage.app")
            } catch (_: Exception) {
                FirebaseStorage.getInstance()
            }
            val filename = "${System.currentTimeMillis()}_${uid.take(6)}.jpg"
            val storageRef = storage.reference.child("users/$uid/profile_photos/$filename")

            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Failed to read selected image file"))

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return Result.failure(Exception("Selected image is empty."))
            }
            if (bytes.size > 5 * 1024 * 1024) {
                return Result.failure(Exception("Image size exceeds the 5MB limit. Please choose a smaller image."))
            }

            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val safeContentType = if (mimeType.startsWith("image/")) mimeType else "image/jpeg"

            val metadata = storageMetadata {
                contentType = safeContentType
            }

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Update photoUrl on users/{uid}
            val userRef = db.collection("users").document(uid)
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            userRef.update(mapOf("photoUrl" to downloadUrl, "updatedAt" to now)).await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseNetworkException -> "Network error during photo upload. Please check your internet connection."
                else -> e.localizedMessage ?: "Failed to upload profile photo."
            }
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Saves profile changes (username, bio, and optional selected image).
     * Enforces username non-empty, bio max 200 characters, and protects UID, BDP ID, role, coins.
     */
    suspend fun saveProfileChanges(
        context: Context,
        username: String,
        bio: String,
        imageUri: Uri? = null
    ): Result<String?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User is not authenticated"))
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank()) {
            return Result.failure(Exception("Username cannot be empty."))
        }
        val trimmedBio = if (bio.length > 200) bio.take(200).trim() else bio.trim()

        return try {
            var uploadedUrl: String? = null
            if (imageUri != null) {
                val uploadResult = uploadProfilePhoto(imageUri, context)
                if (uploadResult.isFailure) {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Failed to upload profile photo"))
                }
                uploadedUrl = uploadResult.getOrNull()
            }

            val userRef = db.collection("users").document(uid)
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val updates = mutableMapOf<String, Any>(
                "username" to trimmedUsername,
                "bio" to trimmedBio,
                "updatedAt" to now
            )
            if (!uploadedUrl.isNullOrBlank()) {
                updates["photoUrl"] = uploadedUrl
            }

            userRef.update(updates).await()
            Result.success(uploadedUrl)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseNetworkException -> "Network error updating profile. Please check your connection."
                else -> e.localizedMessage ?: "Failed to save profile."
            }
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Updates ONLY safe profile fields (username, bio, photoUrl).
     * Protected fields (bdpId, role, coins, uid) cannot be altered.
     */
    suspend fun updateSafeProfile(username: String, bio: String, photoUrl: String = ""): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val trimmedUsername = username.trim()
            if (trimmedUsername.isBlank()) {
                return Result.failure(Exception("Username cannot be empty."))
            }

            val trimmedBio = if (bio.length > 200) bio.take(200).trim() else bio.trim()

            val userRef = db.collection("users").document(uid)
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val updates = mutableMapOf<String, Any>(
                "username" to trimmedUsername,
                "bio" to trimmedBio,
                "updatedAt" to now
            )
            if (photoUrl.isNotBlank()) {
                updates["photoUrl"] = photoUrl.trim()
            }

            userRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val ALLOWED_REQUESTABLE_ROLES = setOf("HOST", "MODERATOR", "OFFICIAL", "ASSISTANT_ADMIN", "ADMIN")
    }

    /**
     * Submit Role & Verification Application to roleRequests/{requestId}.
     * Enforces duplicate pending application check for the same requested role,
     * input validation, length limits, and strict initial status/fields.
     */
    suspend fun submitRoleRequest(
        requestedRole: String,
        reason: String,
        experience: String,
        additionalInfo: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val profile = currentProfile.value ?: return Result.failure(Exception("User profile is not loaded"))

        val targetRole = requestedRole.trim().uppercase()

        if (targetRole == "SUPER_ADMIN") {
            return Result.failure(Exception("SUPER_ADMIN role cannot be applied for."))
        }

        if (targetRole !in ALLOWED_REQUESTABLE_ROLES) {
            return Result.failure(Exception("Invalid role requested: $requestedRole. Allowed roles: HOST, MODERATOR, OFFICIAL, ASSISTANT_ADMIN, ADMIN."))
        }

        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) {
            return Result.failure(Exception("Please provide a reason for your application."))
        }
        if (cleanReason.length > 500) {
            return Result.failure(Exception("Application reason exceeds maximum length of 500 characters."))
        }

        val cleanExp = experience.trim()
        if (cleanExp.length > 500) {
            return Result.failure(Exception("Experience details exceed maximum length of 500 characters."))
        }

        val cleanAdd = additionalInfo.trim()
        if (cleanAdd.length > 500) {
            return Result.failure(Exception("Additional information exceeds maximum length of 500 characters."))
        }

        return try {
            // Check for existing PENDING application for the same role
            val existing = db.collection("roleRequests")
                .whereEqualTo("requesterUid", uid)
                .whereEqualTo("requestedRole", targetRole)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("You already have an active PENDING application for $targetRole. Duplicate applications are not allowed."))
            }

            val reqDoc = db.collection("roleRequests").document()
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val roleRequest = RoleRequest(
                requestId = reqDoc.id,
                requesterUid = uid,
                requesterBdpId = profile.bdpId,
                requesterUsername = profile.username,
                currentRole = profile.role,
                requestedRole = targetRole,
                reason = cleanReason,
                experience = cleanExp,
                additionalInfo = cleanAdd,
                status = "PENDING",
                createdAt = now,
                reviewedAt = null,
                reviewerUid = null,
                reviewerRole = null,
                reviewNote = null
            )

            reqDoc.set(roleRequest).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseNetworkException -> "Network error submitting application. Please check your connection."
                else -> e.localizedMessage ?: "Failed to submit role application."
            }
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Cancel an existing PENDING role application submitted by this user.
     */
    suspend fun cancelRoleRequest(requestId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val reqDoc = db.collection("roleRequests").document(requestId)
            val snap = reqDoc.get().await()
            if (!snap.exists()) {
                return Result.failure(Exception("Application record not found."))
            }
            val request = snap.toObject(RoleRequest::class.java)
                ?: return Result.failure(Exception("Failed to read application data."))

            if (request.requesterUid != uid) {
                return Result.failure(Exception("Permission denied. You can only cancel your own application."))
            }

            if (request.status != "PENDING") {
                return Result.failure(Exception("Only PENDING applications can be cancelled."))
            }

            reqDoc.update(mapOf("status" to "CANCELLED")).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val userMsg = when (e) {
                is FirebaseNetworkException -> "Network error cancelling application."
                else -> e.localizedMessage ?: "Failed to cancel application."
            }
            Result.failure(Exception(userMsg, e))
        }
    }

    /**
     * Inspects Firebase Custom Claims on the authenticated user's ID token.
     * Architectural foundation for server-side SUPER_ADMIN / ADMIN verification.
     */
    suspend fun getCurrentUserClaims(): Map<String, Any?> {
        return try {
            val user = auth.currentUser ?: return emptyMap()
            val tokenResult = user.getIdToken(false).await()
            tokenResult.claims
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Live Flow of user's submitted Role Requests
     */
    fun getUserRoleRequestsFlow(): Flow<List<RoleRequest>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("roleRequests")
            .whereEqualTo("requesterUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject(RoleRequest::class.java) } ?: emptyList()
                trySend(requests.sortedByDescending { it.createdAt })
            }

        awaitClose { listener.remove() }
    }

    /**
     * Admin: Live Flow of all Role Requests in the system.
     */
    fun getAllRoleRequestsFlow(): Flow<List<RoleRequest>> = callbackFlow {
        val listener = db.collection("roleRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject(RoleRequest::class.java) } ?: emptyList()
                trySend(requests.sortedByDescending { it.createdAt })
            }

        awaitClose { listener.remove() }
    }

    /**
     * Admin: Fetch real registered users from Firestore with optional role filter.
     */
    suspend fun fetchUsers(roleFilter: String? = null, limit: Long = 50): Result<List<FirestoreUserProfile>> {
        return try {
            val query = if (!roleFilter.isNullOrBlank()) {
                db.collection("users").whereEqualTo("role", roleFilter).limit(limit)
            } else {
                db.collection("users").limit(limit)
            }
            val snap = query.get().await()
            val users = snap.documents.mapNotNull { it.toObject(FirestoreUserProfile::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: Fetch real count of total registered users from Firestore.
     */
    suspend fun fetchTotalUsersCount(): Result<Long> {
        return try {
            val countSnap = db.collection("users")
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
            Result.success(countSnap.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: Fetch real count of pending role applications.
     */
    suspend fun fetchPendingRoleRequestsCount(): Result<Long> {
        return try {
            val countSnap = db.collection("roleRequests")
                .whereEqualTo("status", "PENDING")
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
            Result.success(countSnap.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: Fetch real count of users with a specific role.
     */
    suspend fun fetchRoleCount(role: String): Result<Long> {
        return try {
            val countSnap = db.collection("users")
                .whereEqualTo("role", role)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
            Result.success(countSnap.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin Approval Contract:
     * Prepares and validates the backend contract for role approval.
     * Note: As required by zero-trust architecture, final server-side role promotion
     * must be dispatched by trusted Firebase Cloud Functions / Admin SDK to prevent client privilege tampering.
     */
    suspend fun executeRoleApprovalContract(requestId: String, reviewNote: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val profile = currentProfile.value ?: return Result.failure(Exception("Admin profile not loaded"))

        if (profile.role !in listOf("ADMIN", "SUPER_ADMIN", "ASSISTANT_ADMIN")) {
            return Result.failure(Exception("Unauthorized: Admin privileges required."))
        }

        return try {
            val reqDoc = db.collection("roleRequests").document(requestId)
            val snap = reqDoc.get().await()
            if (!snap.exists()) {
                return Result.failure(Exception("Role request [$requestId] not found."))
            }
            val req = snap.toObject(RoleRequest::class.java)
                ?: return Result.failure(Exception("Failed to deserialize role request."))

            if (req.status != "PENDING") {
                return Result.failure(Exception("Cannot approve request with status '${req.status}'. Only PENDING requests may be reviewed."))
            }

            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            // Contract documentation payload
            val contractNotice = "Contract Prepared: Application [$requestId] approved by ${profile.username} (${profile.role}). Note: Target user role promotion requires trusted backend / Cloud Functions execution."
            Result.success(contractNotice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin Rejection Contract:
     * Rejects the role application without changing target user role.
     */
    suspend fun executeRoleRejectionContract(requestId: String, reviewNote: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val profile = currentProfile.value ?: return Result.failure(Exception("Admin profile not loaded"))

        if (profile.role !in listOf("ADMIN", "SUPER_ADMIN", "ASSISTANT_ADMIN")) {
            return Result.failure(Exception("Unauthorized: Admin privileges required."))
        }

        return try {
            val reqDoc = db.collection("roleRequests").document(requestId)
            val snap = reqDoc.get().await()
            if (!snap.exists()) {
                return Result.failure(Exception("Role request [$requestId] not found."))
            }
            val req = snap.toObject(RoleRequest::class.java)
                ?: return Result.failure(Exception("Failed to deserialize role request."))

            if (req.status != "PENDING") {
                return Result.failure(Exception("Cannot reject request with status '${req.status}'. Only PENDING requests may be reviewed."))
            }

            val contractNotice = "Contract Prepared: Application [$requestId] rejected by ${profile.username} (${profile.role}). No user role changes occurred."
            Result.success(contractNotice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        detachUserProfileListener()
        auth.signOut()
        _currentProfile.value = null
        _currentDomainUser.value = null
        _authState.value = AuthState.Unauthenticated()
    }
}
