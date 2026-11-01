package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.wallet.WalletRepository
import com.example.model.FirestorePaymentRequest
import com.example.model.PaymentRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BD PARTY", appName)
  }

  @Test
  fun `default coin packages are defined and enabled`() {
    val repo = WalletRepository()
    val packages = repo.defaultCoinPackages()
    assertTrue(packages.isNotEmpty())
    assertEquals("Starter Pack", packages.first().name)
    assertTrue(packages.all { it.enabled })
    assertTrue(packages.all { it.coinAmount > 0 })
  }

  @Test
  fun `manual payment request initializes with pending status`() {
    val req = FirestorePaymentRequest(
      requestId = "req_123",
      uid = "user_abc",
      bdpId = "BDP889900",
      username = "TestUser",
      paymentMethod = "bKash",
      packageId = "pkg_1",
      coinAmount = 10000L,
      amount = "৳120",
      senderNumber = "01700112233",
      transactionId = "9J4K2L8",
      status = PaymentRequestStatus.PENDING.name
    )
    assertEquals(PaymentRequestStatus.PENDING.name, req.status)
    assertEquals("bKash", req.paymentMethod)
    val map = req.toMap()
    assertEquals("PENDING", map["status"])
    assertEquals("9J4K2L8", map["transactionId"])
  }

  @Test
  fun `default gift catalog has valid items and positive coin prices`() {
    val giftRepo = com.example.data.gift.GiftRepository()
    val gifts = giftRepo.defaultGifts()
    assertTrue(gifts.isNotEmpty())
    assertTrue(gifts.all { it.giftId.isNotBlank() })
    assertTrue(gifts.all { it.name.isNotBlank() })
    assertTrue(gifts.all { it.coinPrice > 0 })
    assertEquals("Rose", gifts.first().name)
    assertEquals(10L, gifts.first().coinPrice)
  }

  @Test
  fun `gift validation rejects self-gifting`() {
    val giftRepo = com.example.data.gift.GiftRepository()
    val error = giftRepo.validateGiftRequest(
      receiverUid = "user_me",
      currentUserUid = "user_me",
      quantity = 1
    )
    assertEquals("Cannot send a gift to yourself", error)
  }

  @Test
  fun `gift validation rejects invalid or blank receiver`() {
    val giftRepo = com.example.data.gift.GiftRepository()
    val error = giftRepo.validateGiftRequest(
      receiverUid = "",
      currentUserUid = "user_me",
      quantity = 1
    )
    assertEquals("Please select a valid stage member", error)
  }

  @Test
  fun `gift validation enforces quantity limits between 1 and 9999`() {
    val giftRepo = com.example.data.gift.GiftRepository()

    // Test zero quantity
    val errZero = giftRepo.validateGiftRequest("user_other", "user_me", 0)
    assertEquals("Quantity must be greater than 0", errZero)

    // Test negative quantity
    val errNeg = giftRepo.validateGiftRequest("user_other", "user_me", -5)
    assertEquals("Quantity must be greater than 0", errNeg)

    // Test excessive quantity > 9999
    val errOver = giftRepo.validateGiftRequest("user_other", "user_me", 10000)
    assertEquals("Maximum gift quantity is 9,999", errOver)

    // Test valid boundary quantities
    val valid1 = giftRepo.validateGiftRequest("user_other", "user_me", 1)
    val valid99 = giftRepo.validateGiftRequest("user_other", "user_me", 99)
    val valid9999 = giftRepo.validateGiftRequest("user_other", "user_me", 9999)
    org.junit.Assert.assertNull(valid1)
    org.junit.Assert.assertNull(valid99)
    org.junit.Assert.assertNull(valid9999)
  }

  @Test
  fun `gift transaction model initializes with pending status and correct totals`() {
    val txn = com.example.model.FirestoreGiftTransaction(
      transactionId = "tx_9988",
      senderUid = "u_sender",
      senderBdpId = "BDP1111",
      senderUsername = "SenderName",
      receiverUid = "u_receiver",
      receiverBdpId = "BDP2222",
      receiverUsername = "ReceiverName",
      giftId = "g_rose",
      giftName = "Rose",
      coinPrice = 10L,
      quantity = 66,
      totalCoins = 660L,
      roomId = "room_live_1",
      status = com.example.model.GiftTransactionStatus.PENDING.name
    )
    assertEquals("PENDING", txn.status)
    assertEquals(660L, txn.totalCoins)
    val map = txn.toMap()
    assertEquals("PENDING", map["status"])
    assertEquals("room_live_1", map["roomId"])
    assertEquals("Rose", map["giftName"])
    assertEquals(66, map["quantity"])
  }

  @Test
  fun `room categories are complete and properly labeled`() {
    val categories = com.example.model.RoomCategory.values()
    assertEquals(5, categories.size)
    assertTrue(categories.any { it.name == "PARTY" && it.displayName == "Party" })
    assertTrue(categories.any { it.name == "SINGING" && it.displayName == "Singing" })
    assertTrue(categories.any { it.name == "GAMING" && it.displayName == "Gaming" })
    assertTrue(categories.any { it.name == "CHAT" && it.displayName == "Chat" })
  }

  @Test
  fun `design tokens spacing and shapes are positive and defined`() {
    assertTrue(com.example.ui.theme.BdSpacing.xs.value > 0)
    assertTrue(com.example.ui.theme.BdSpacing.sm.value > 0)
    assertTrue(com.example.ui.theme.BdSpacing.lg.value > 0)
    assertEquals(200, com.example.ui.theme.BdMotion.fastDurationMs)
  }
}

