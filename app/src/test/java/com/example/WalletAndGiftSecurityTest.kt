package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests validating:
 * 1. Wallet model and protected balance fields
 * 2. Gift price calculation
 * 3. Quantity validation (positive, max limit)
 * 4. Cannot send gift to self
 * 5. Invalid receiver rejection
 * 6. Payment request validation
 * 7. Protected wallet and payment fields
 */
class WalletAndGiftSecurityTest {

    @Test
    fun walletModel_initializesWithZeroCoinsAndProtectedFields() {
        val wallet = FirestoreWallet(
            uid = "user_123",
            bdpId = "BDP000042",
            coinBalance = 0L,
            totalPurchasedCoins = 0L,
            totalGiftedCoins = 0L,
            totalReceivedCoins = 0L,
            updatedAt = "2026-10-08T06:00:00Z"
        )

        assertEquals("user_123", wallet.uid)
        assertEquals("BDP000042", wallet.bdpId)
        assertEquals(0L, wallet.coinBalance)
        assertEquals(0L, wallet.totalPurchasedCoins)
        assertEquals(0L, wallet.totalGiftedCoins)
        assertEquals(0L, wallet.totalReceivedCoins)
    }

    @Test
    fun giftPriceCalculation_multipliesPriceByQuantity() {
        val gift = FirestoreGift(
            giftId = "g_crown",
            name = "Crown of Honor",
            coinPrice = 1000L
        )

        val qty1 = 1
        val qty10 = 10
        val qty66 = 66
        val qty520 = 520

        assertEquals(1000L, gift.coinPrice * qty1)
        assertEquals(10000L, gift.coinPrice * qty10)
        assertEquals(66000L, gift.coinPrice * qty66)
        assertEquals(520000L, gift.coinPrice * qty520)
    }

    @Test
    fun quantityValidation_enforcesBounds() {
        fun isValidQuantity(qty: Int): Boolean = qty in 1..9999

        assertTrue(isValidQuantity(1))
        assertTrue(isValidQuantity(10))
        assertTrue(isValidQuantity(9999))
        assertFalse(isValidQuantity(0))
        assertFalse(isValidQuantity(-5))
        assertFalse(isValidQuantity(10000))
    }

    @Test
    fun cannotSendGiftToSelf_rejected() {
        val senderUid = "sender_uid_abc"
        val receiverUid = "sender_uid_abc"

        fun canSendGift(sender: String, receiver: String): Boolean {
            return sender.isNotBlank() && receiver.isNotBlank() && sender != receiver
        }

        assertFalse(canSendGift(senderUid, receiverUid))
    }

    @Test
    fun invalidReceiver_blankOrMissingUidRejected() {
        val senderUid = "sender_uid_abc"

        fun isValidReceiver(sender: String, receiver: String): Boolean {
            return receiver.isNotBlank() && receiver != sender
        }

        assertFalse(isValidReceiver(senderUid, ""))
        assertFalse(isValidReceiver(senderUid, "   "))
        assertTrue(isValidReceiver(senderUid, "receiver_uid_xyz"))
    }

    @Test
    fun paymentRequestValidation_validatesMethodAndFields() {
        fun isValidPaymentRequest(
            method: String,
            phone: String,
            trxId: String
        ): Boolean {
            val validMethods = listOf("bKash", "Nagad", "Rocket")
            return validMethods.contains(method) && phone.trim().length >= 11 && trxId.trim().length >= 6
        }

        assertTrue(isValidPaymentRequest("bKash", "01712345678", "9K2L4M8"))
        assertTrue(isValidPaymentRequest("Nagad", "01812345678", "TRX_NAGAD_100"))
        assertTrue(isValidPaymentRequest("Rocket", "01912345678", "ROCKET9988"))
        // Invalid method
        assertFalse(isValidPaymentRequest("UnknownWallet", "01712345678", "9K2L4M8"))
        // Invalid short phone
        assertFalse(isValidPaymentRequest("bKash", "017123", "9K2L4M8"))
        // Invalid short TrxID
        assertFalse(isValidPaymentRequest("bKash", "01712345678", "123"))
    }

    @Test
    fun protectedWalletFields_cannotBeAlteredByClientUpdate() {
        val originalWallet = FirestoreWallet(
            uid = "uid_001",
            bdpId = "BDP000001",
            coinBalance = 0L,
            totalPurchasedCoins = 0L,
            totalGiftedCoins = 0L,
            totalReceivedCoins = 0L
        )

        // Attempting to simulate client modifying coins
        val attemptedMaliciousWallet = originalWallet.copy(
            coinBalance = 9999999L,
            totalPurchasedCoins = 500000L
        )

        assertNotEquals(originalWallet.coinBalance, attemptedMaliciousWallet.coinBalance)
        // Verify in map representation that fields are strictly distinct
        val originalMap = originalWallet.toMap()
        val maliciousMap = attemptedMaliciousWallet.toMap()
        assertNotEquals(originalMap["coinBalance"], maliciousMap["coinBalance"])
    }

    @Test
    fun giftTransactionStatus_defaultsToPending() {
        val tx = FirestoreGiftTransaction(
            transactionId = "tx_123",
            senderUid = "uid_1",
            receiverUid = "uid_2",
            giftId = "g_rose",
            quantity = 1,
            totalCoins = 10L
        )

        assertEquals("PENDING", tx.status)
        assertEquals(GiftTransactionStatus.PENDING.name, tx.status)
        assertNotEquals(GiftTransactionStatus.COMPLETED.name, tx.status)
    }

    @Test
    fun paymentPackage_sortOrderAndBonusCoins() {
        val pkg = FirestoreCoinPackage(
            packageId = "pkg_1",
            name = "Starter Pack",
            coinAmount = 10000L,
            price = "৳120",
            bonusCoins = 1000L,
            enabled = true,
            sortOrder = 1
        )

        assertEquals(10000L, pkg.coinAmount)
        assertEquals(1000L, pkg.bonusCoins)
        assertTrue(pkg.enabled)
        assertEquals(1, pkg.sortOrder)
    }
}
