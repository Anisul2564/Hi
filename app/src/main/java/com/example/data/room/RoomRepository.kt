package com.example.data.room

import android.content.Context
import com.example.R
import com.example.model.FirestoreRoom
import com.example.model.MicState
import com.example.model.RoomParticipant
import com.example.model.RoomRole
import com.example.model.RoomStatus
import com.example.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RoomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun currentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Creates a new voice room document and registers the authenticated creator as the initial HOST.
     * Host fields are strictly populated from authenticated session data.
     */
    suspend fun createRoom(
        title: String,
        description: String,
        category: String,
        visibility: String,
        currentUser: User,
        backgroundUrl: String = "",
        coverUrl: String = ""
    ): Result<String> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val roomId = "BD" + (100000 + (Math.random() * 900000).toInt()).toString()
        val now = currentTimestamp()

        val roomDoc = FirestoreRoom(
            roomId = roomId,
            hostUid = firebaseUser.uid,
            hostBdpId = currentUser.bdpId.ifBlank { "BDP-${firebaseUser.uid.take(6)}" },
            hostUsername = currentUser.displayName.ifBlank { currentUser.username.ifBlank { "Host" } },
            title = title.trim(),
            description = description.trim(),
            category = category,
            visibility = visibility,
            status = RoomStatus.LIVE.name,
            maxSpeakers = 12,
            createdAt = now,
            updatedAt = now,
            participantCount = 1,
            listenerCount = 0,
            backgroundUrl = backgroundUrl,
            coverUrl = coverUrl
        )

        val hostParticipant = RoomParticipant(
            uid = firebaseUser.uid,
            bdpId = roomDoc.hostBdpId,
            username = roomDoc.hostUsername,
            photoUrl = currentUser.photoUrl,
            role = currentUser.role.name,
            roomRole = RoomRole.HOST.name,
            micState = MicState.UNMUTED.name,
            seatIndex = 0, // Host automatically takes Seat #0
            joinedAt = now,
            updatedAt = now
        )

        return try {
            val roomRef = db.collection("rooms").document(roomId)
            val participantRef = roomRef.collection("participants").document(firebaseUser.uid)

            db.runBatch { batch ->
                batch.set(roomRef, roomDoc)
                batch.set(participantRef, hostParticipant)
            }.await()

            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of active live rooms from Firestore.
     */
    fun getLiveRoomsFlow(): Flow<List<FirestoreRoom>> = callbackFlow {
        val query = db.collection("rooms")
            .whereEqualTo("status", RoomStatus.LIVE.name)
            .whereEqualTo("visibility", "PUBLIC")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If index is building or query fails, emit empty list rather than crashing
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val rooms = snapshot.documents.mapNotNull { it.toObject(FirestoreRoom::class.java) }
                trySend(rooms)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of a single room document.
     */
    fun getRoomFlow(roomId: String): Flow<FirestoreRoom?> = callbackFlow {
        val docRef = db.collection("rooms").document(roomId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(FirestoreRoom::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of all participants inside a room.
     */
    fun getParticipantsFlow(roomId: String): Flow<List<RoomParticipant>> = callbackFlow {
        val collection = db.collection("rooms").document(roomId).collection("participants")
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val participants = snapshot.documents.mapNotNull { it.toObject(RoomParticipant::class.java) }
                trySend(participants)
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Join room as a listener.
     */
    suspend fun joinRoom(roomId: String, currentUser: User): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val now = currentTimestamp()
        val participantRef = db.collection("rooms").document(roomId)
            .collection("participants").document(firebaseUser.uid)
        val roomRef = db.collection("rooms").document(roomId)

        return try {
            val existing = participantRef.get().await()
            val userProfileSnap = db.collection("users").document(firebaseUser.uid).get().await()
            val effectivePhoto = if (userProfileSnap.exists()) {
                userProfileSnap.getString("photoUrl")?.takeIf { it.isNotBlank() } ?: currentUser.photoUrl
            } else currentUser.photoUrl
            val effectiveName = if (userProfileSnap.exists()) {
                userProfileSnap.getString("username")?.takeIf { it.isNotBlank() } ?: currentUser.displayName.ifBlank { currentUser.username.ifBlank { "User" } }
            } else currentUser.displayName.ifBlank { currentUser.username.ifBlank { "User" } }

            if (!existing.exists()) {
                val participant = RoomParticipant(
                    uid = firebaseUser.uid,
                    bdpId = currentUser.bdpId.ifBlank { "BDP-${firebaseUser.uid.take(6)}" },
                    username = effectiveName,
                    photoUrl = effectivePhoto,
                    role = currentUser.role.name,
                    roomRole = RoomRole.LISTENER.name,
                    micState = MicState.MUTED.name,
                    seatIndex = -1,
                    joinedAt = now,
                    updatedAt = now
                )
                db.runBatch { batch ->
                    batch.set(participantRef, participant)
                    batch.update(
                        roomRef,
                        mapOf(
                            "participantCount" to FieldValue.increment(1),
                            "listenerCount" to FieldValue.increment(1),
                            "updatedAt" to now
                        )
                    )
                }.await()
            } else {
                val updates = mutableMapOf<String, Any>("updatedAt" to now)
                if (effectivePhoto.isNotBlank()) updates["photoUrl"] = effectivePhoto
                if (effectiveName.isNotBlank()) updates["username"] = effectiveName
                participantRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Take an open speaker seat (0..11) or release current seat atomically using a Firestore transaction.
     * Verifies:
     * - User is authenticated
     * - Room exists and is currently LIVE
     * - seatIndex is strictly within range 0..11
     * - Supports taking an open seat or smoothly changing seats
     * - Prevents duplicate seat assignment or client spoofing of roomRole
     */
    suspend fun occupySeat(roomId: String, targetSeatIndex: Int): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (targetSeatIndex < 0 || targetSeatIndex >= 12) {
            return Result.failure(IllegalArgumentException("Invalid seat index. Stage supports 12 seats (0..11)."))
        }

        val roomRef = db.collection("rooms").document(roomId)
        val participantRef = roomRef.collection("participants").document(firebaseUser.uid)
        val userRef = db.collection("users").document(firebaseUser.uid)

        return try {
            db.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                if (!roomSnapshot.exists()) {
                    throw IllegalStateException("Room does not exist.")
                }
                val roomStatus = roomSnapshot.getString("status") ?: RoomStatus.ENDED.name
                if (roomStatus != RoomStatus.LIVE.name) {
                    throw IllegalStateException("Cannot take a seat: Room is not LIVE.")
                }
                val hostUid = roomSnapshot.getString("hostUid") ?: ""

                // Check caller's participant snapshot
                val callerSnapshot = transaction.get(participantRef)
                if (!callerSnapshot.exists()) {
                    throw IllegalStateException("User has not joined this room audience.")
                }

                // Check caller's profile snapshot for real profile picture
                val userProfileSnapshot = transaction.get(userRef)
                val userPhoto = if (userProfileSnapshot.exists()) {
                    userProfileSnapshot.getString("photoUrl") ?: ""
                } else ""
                val userName = if (userProfileSnapshot.exists()) {
                    userProfileSnapshot.getString("username") ?: ""
                } else ""

                val currentSeat = callerSnapshot.getLong("seatIndex")?.toInt() ?: -1
                if (currentSeat == targetSeatIndex) {
                    // Already in this seat
                    return@runTransaction
                }

                val callerRole = callerSnapshot.getString("role") ?: "USER"
                val existingRoomRole = callerSnapshot.getString("roomRole") ?: RoomRole.LISTENER.name

                // Assign appropriate room role: host retains HOST; co-hosts retain CO_HOST; listeners become SPEAKER
                val newRoomRole = when {
                    firebaseUser.uid == hostUid -> RoomRole.HOST.name
                    existingRoomRole == RoomRole.CO_HOST.name -> RoomRole.CO_HOST.name
                    existingRoomRole == RoomRole.MODERATOR.name -> RoomRole.MODERATOR.name
                    else -> RoomRole.SPEAKER.name
                }

                val now = currentTimestamp()
                val updates = mutableMapOf<String, Any>(
                    "seatIndex" to targetSeatIndex,
                    "roomRole" to newRoomRole,
                    "updatedAt" to now
                )
                if (userPhoto.isNotBlank()) {
                    updates["photoUrl"] = userPhoto
                }
                if (userName.isNotBlank()) {
                    updates["username"] = userName
                }
                transaction.update(participantRef, updates)

                // If user was previously counted as listener, decrement listener count
                if (existingRoomRole == RoomRole.LISTENER.name && currentSeat !in 0..11) {
                    transaction.update(roomRef, "listenerCount", FieldValue.increment(-1))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave seat and return to listener audience atomically.
     * Prevents clients from arbitrary self-promotion to HOST.
     */
    suspend fun releaseSeat(roomId: String): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val roomRef = db.collection("rooms").document(roomId)
        val participantRef = roomRef.collection("participants").document(firebaseUser.uid)

        return try {
            db.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val hostUid = roomSnapshot.getString("hostUid") ?: ""
                val callerSnapshot = transaction.get(participantRef)

                if (callerSnapshot.exists()) {
                    val currentSeat = callerSnapshot.getLong("seatIndex")?.toInt() ?: -1
                    val existingRoomRole = callerSnapshot.getString("roomRole") ?: RoomRole.LISTENER.name

                    // Host remains HOST role even if they step off mic seat; others revert to LISTENER
                    val newRoomRole = if (firebaseUser.uid == hostUid) {
                        RoomRole.HOST.name
                    } else if (existingRoomRole == RoomRole.CO_HOST.name) {
                        RoomRole.CO_HOST.name
                    } else if (existingRoomRole == RoomRole.MODERATOR.name) {
                        RoomRole.MODERATOR.name
                    } else {
                        RoomRole.LISTENER.name
                    }

                    val now = currentTimestamp()
                    transaction.update(
                        participantRef,
                        mapOf(
                            "seatIndex" to -1,
                            "roomRole" to newRoomRole,
                            "micState" to MicState.MUTED.name,
                            "updatedAt" to now
                        )
                    )

                    if (currentSeat in 0..11 && newRoomRole == RoomRole.LISTENER.name) {
                        transaction.update(roomRef, "listenerCount", FieldValue.increment(1))
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Transition participant room role (host/moderator authorized only).
     * Supported transitions:
     * - SPEAKER -> CO_HOST
     * - CO_HOST -> SPEAKER
     * - Any -> MODERATOR (host only)
     * - MODERATOR -> LISTENER/SPEAKER (host only)
     */
    suspend fun changeParticipantRoomRole(roomId: String, targetUid: String, newRole: RoomRole): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val roomRef = db.collection("rooms").document(roomId)
        val callerRef = roomRef.collection("participants").document(firebaseUser.uid)
        val targetRef = roomRef.collection("participants").document(targetUid)

        return try {
            db.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef)
                val hostUid = roomSnapshot.getString("hostUid") ?: ""
                val callerSnapshot = transaction.get(callerRef)

                val callerRoomRole = callerSnapshot.getString("roomRole") ?: RoomRole.LISTENER.name
                val isHost = firebaseUser.uid == hostUid
                val isModerator = callerRoomRole == RoomRole.MODERATOR.name || callerRoomRole == RoomRole.CO_HOST.name

                if (!isHost && !isModerator) {
                    throw SecurityException("Only the room host or moderator can assign room roles.")
                }

                // Only the room host can appoint or demote CO_HOST or MODERATOR
                if ((newRole == RoomRole.CO_HOST || newRole == RoomRole.MODERATOR) && !isHost) {
                    throw SecurityException("Only the room host can appoint Co-Hosts or Room Moderators.")
                }

                // Nobody can change the room host's role
                if (targetUid == hostUid && newRole != RoomRole.HOST) {
                    throw SecurityException("Cannot alter the room host's role.")
                }

                transaction.update(
                    targetRef,
                    mapOf(
                        "roomRole" to newRole.name,
                        "updatedAt" to currentTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update participant's own microphone state.
     */
    suspend fun setMicState(roomId: String, micState: MicState): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val participantRef = db.collection("rooms").document(roomId)
            .collection("participants").document(firebaseUser.uid)
        val now = currentTimestamp()

        return try {
            participantRef.update(
                mapOf(
                    "micState" to micState.name,
                    "updatedAt" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave room cleanly.
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val participantRef = db.collection("rooms").document(roomId)
            .collection("participants").document(firebaseUser.uid)
        val roomRef = db.collection("rooms").document(roomId)

        return try {
            val doc = participantRef.get().await()
            if (doc.exists()) {
                val wasListener = doc.getString("roomRole") == RoomRole.LISTENER.name
                db.runBatch { batch ->
                    batch.delete(participantRef)
                    val updates = mutableMapOf<String, Any>(
                        "participantCount" to FieldValue.increment(-1),
                        "updatedAt" to currentTimestamp()
                    )
                    if (wasListener) {
                        updates["listenerCount"] = FieldValue.increment(-1)
                    }
                    batch.update(roomRef, updates)
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Host ends room. Sets status = ENDED.
     */
    suspend fun endRoom(roomId: String): Result<Unit> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        val roomRef = db.collection("rooms").document(roomId)
        val now = currentTimestamp()

        return try {
            val snapshot = roomRef.get().await()
            if (snapshot.getString("hostUid") != firebaseUser.uid) {
                return Result.failure(SecurityException("Only the room host can end this stage."))
            }

            roomRef.update(
                mapOf(
                    "status" to RoomStatus.ENDED.name,
                    "updatedAt" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Host/Moderator mutes or manages participant.
     */
    suspend fun moderatorMuteParticipant(roomId: String, targetUid: String, newMicState: MicState): Result<Unit> {
        val participantRef = db.collection("rooms").document(roomId)
            .collection("participants").document(targetUid)
        return try {
            participantRef.update(
                mapOf(
                    "micState" to newMicState.name,
                    "updatedAt" to currentTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Host/Moderator kicks participant from mic seat back to audience.
     */
    suspend fun moderatorKickFromSeat(roomId: String, targetUid: String): Result<Unit> {
        val participantRef = db.collection("rooms").document(roomId)
            .collection("participants").document(targetUid)
        return try {
            participantRef.update(
                mapOf(
                    "seatIndex" to -1,
                    "roomRole" to RoomRole.LISTENER.name,
                    "micState" to MicState.MUTED.name,
                    "updatedAt" to currentTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
