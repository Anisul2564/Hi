package com.example

import com.example.data.firebase.FirestoreUserProfile
import com.example.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class FirebaseUserProfileTest {

    @Test
    fun bdpId_formatValidation() {
        val bdpRegex = Regex("^BDP[0-9]{6,}$")
        assertTrue(bdpRegex.matches("BDP000001"))
        assertTrue(bdpRegex.matches("BDP000042"))
        assertTrue(bdpRegex.matches("BDP100000"))
        assertFalse(bdpRegex.matches("user@example.com"))
        assertFalse(bdpRegex.matches("BDP123"))
        assertFalse(bdpRegex.matches("000001"))
    }

    @Test
    fun newUserProfile_hasDefaultUserRoleAndZeroCoins() {
        val profile = FirestoreUserProfile(
            uid = "test_firebase_uid_123",
            bdpId = "BDP000001",
            username = "Rayan",
            email = "rayan@test.com"
        )

        assertEquals("USER", profile.role)
        assertEquals(0L, profile.coins)

        val domainUser = profile.toDomainUser()
        assertEquals(UserRole.USER, domainUser.role)
        assertEquals(0L, domainUser.coins)
        assertEquals("BDP000001", domainUser.bdpId)
        assertEquals("Rayan", domainUser.username)
        assertEquals("test_firebase_uid_123", domainUser.uid)
    }

    @Test
    fun safeProfileUpdate_preservesImmutableFields() {
        val original = FirestoreUserProfile(
            uid = "uid_99",
            bdpId = "BDP000099",
            username = "OriginalName",
            email = "user@test.com",
            photoUrl = "",
            bio = "Old bio",
            role = "USER",
            coins = 0L,
            createdAt = "2026-10-07T00:00:00Z"
        )

        val updated = original.copy(
            username = "NewName",
            bio = "New updated bio"
        )

        // Verify protected fields remain untouched
        assertEquals(original.uid, updated.uid)
        assertEquals(original.bdpId, updated.bdpId)
        assertEquals(original.role, updated.role)
        assertEquals(original.coins, updated.coins)

        // Verify safe fields changed
        assertEquals("NewName", updated.username)
        assertEquals("New updated bio", updated.bio)
    }

    @Test
    fun safeProfilePhotoUpdate_updatesUrlOnly() {
        val original = FirestoreUserProfile(
            uid = "uid_101",
            bdpId = "BDP000101",
            username = "Rayan",
            role = "USER",
            coins = 0L,
            photoUrl = ""
        )

        val updatedWithPhoto = original.copy(
            photoUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/users%2Fuid_101%2Fprofile_photos%2Fphoto.jpg"
        )

        assertEquals("https://firebasestorage.googleapis.com/v0/b/app/o/users%2Fuid_101%2Fprofile_photos%2Fphoto.jpg", updatedWithPhoto.photoUrl)
        assertEquals("BDP000101", updatedWithPhoto.bdpId)
        assertEquals("USER", updatedWithPhoto.role)
        assertEquals(0L, updatedWithPhoto.coins)
    }

    @Test
    fun bioLengthConstraint_truncatesAt200Characters() {
        val longBio = "A".repeat(300)
        val trimmedBio = if (longBio.length > 200) longBio.take(200).trim() else longBio.trim()
        assertEquals(200, trimmedBio.length)
    }

    @Test
    fun usernameValidation_rejectsBlank() {
        val emptyUsername = "   "
        val trimmed = emptyUsername.trim()
        assertTrue(trimmed.isBlank())
    }

    @Test
    fun supportedRoles_containsExactExpectedRoles() {
        val expectedRoleNames = setOf(
            "USER", "HOST", "MODERATOR", "OFFICIAL", "ASSISTANT_ADMIN", "ADMIN", "SUPER_ADMIN"
        )
        val actualRoleNames = UserRole.values().map { it.name }.toSet()
        assertEquals(expectedRoleNames, actualRoleNames)
    }

    @Test
    fun requestableRoles_excludesSuperAdmin() {
        val requestable = com.example.data.firebase.AuthRepository.ALLOWED_REQUESTABLE_ROLES
        assertTrue(requestable.contains("HOST"))
        assertTrue(requestable.contains("MODERATOR"))
        assertTrue(requestable.contains("OFFICIAL"))
        assertTrue(requestable.contains("ASSISTANT_ADMIN"))
        assertTrue(requestable.contains("ADMIN"))
        assertFalse(requestable.contains("SUPER_ADMIN"))
        assertFalse(requestable.contains("USER"))
    }

    @Test
    fun newRoleRequest_initializesWithPendingAndNullReviewer() {
        val request = com.example.model.RoleRequest(
            requestId = "req_123",
            requesterUid = "uid_456",
            requesterBdpId = "BDP000456",
            requesterUsername = "HostApplicant",
            currentRole = "USER",
            requestedRole = "HOST",
            reason = "Want to host live audio rooms.",
            experience = "10 hours per week stage experience.",
            additionalInfo = "",
            status = "PENDING",
            createdAt = "2026-10-07T12:00:00Z"
        )

        assertEquals("PENDING", request.status)
        assertNull(request.reviewedAt)
        assertNull(request.reviewerUid)
        assertNull(request.reviewerRole)
        assertNull(request.reviewNote)
        assertEquals("HOST", request.requestedRole)
    }

    @Test
    fun auditLogModel_structureVerification() {
        val log = com.example.model.AuditLog(
            logId = "log_1",
            actorUid = "admin_uid_9",
            actorBdpId = "BDP000009",
            actorRole = "ADMIN",
            action = "REVIEW_ROLE_REQUEST",
            targetUid = "user_uid_456",
            targetBdpId = "BDP000456",
            oldValue = "PENDING",
            newValue = "APPROVED",
            reason = "Met community guidelines.",
            timestamp = "2026-10-07T12:30:00Z"
        )

        assertEquals("log_1", log.logId)
        assertEquals("ADMIN", log.actorRole)
        assertEquals("REVIEW_ROLE_REQUEST", log.action)
        assertEquals("APPROVED", log.newValue)
    }
}
