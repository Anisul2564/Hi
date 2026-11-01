package com.example.model

/**
 * Production-ready Firestore models for Wallet, Gifts, Transactions, and Manual Payment Requests.
 */

data class FirestoreWallet(
    val uid: String = "",
    val bdpId: String = "",
    val coinBalance: Long = 0L,
    val totalPurchasedCoins: Long = 0L,
    val totalGiftedCoins: Long = 0L,
    val totalReceivedCoins: Long = 0L,
    val updatedAt: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "bdpId" to bdpId,
        "coinBalance" to coinBalance,
        "totalPurchasedCoins" to totalPurchasedCoins,
        "totalGiftedCoins" to totalGiftedCoins,
        "totalReceivedCoins" to totalReceivedCoins,
        "updatedAt" to updatedAt
    )
}

data class FirestoreGift(
    val giftId: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val animationUrl: String = "",
    val coinPrice: Long = 0L,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "giftId" to giftId,
        "name" to name,
        "description" to description,
        "iconUrl" to iconUrl,
        "animationUrl" to animationUrl,
        "coinPrice" to coinPrice,
        "enabled" to enabled,
        "sortOrder" to sortOrder,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

enum class GiftTransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class FirestoreGiftTransaction(
    val transactionId: String = "",
    val senderUid: String = "",
    val senderBdpId: String = "",
    val senderUsername: String = "",
    val receiverUid: String = "",
    val receiverBdpId: String = "",
    val receiverUsername: String = "",
    val giftId: String = "",
    val giftName: String = "",
    val coinPrice: Long = 0L,
    val quantity: Int = 1,
    val totalCoins: Long = 0L,
    val roomId: String = "",
    val createdAt: String = "",
    val status: String = GiftTransactionStatus.PENDING.name
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "transactionId" to transactionId,
        "senderUid" to senderUid,
        "senderBdpId" to senderBdpId,
        "senderUsername" to senderUsername,
        "receiverUid" to receiverUid,
        "receiverBdpId" to receiverBdpId,
        "receiverUsername" to receiverUsername,
        "giftId" to giftId,
        "giftName" to giftName,
        "coinPrice" to coinPrice,
        "quantity" to quantity,
        "totalCoins" to totalCoins,
        "roomId" to roomId,
        "createdAt" to createdAt,
        "status" to status
    )
}

enum class PaymentRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

data class FirestorePaymentRequest(
    val requestId: String = "",
    val uid: String = "",
    val bdpId: String = "",
    val username: String = "",
    val paymentMethod: String = "", // "bKash", "Nagad", "Rocket"
    val packageId: String = "",
    val coinAmount: Long = 0L,
    val amount: String = "", // e.g. "৳120" or "$0.99"
    val senderNumber: String = "",
    val transactionId: String = "",
    val screenshotUrl: String = "",
    val status: String = PaymentRequestStatus.PENDING.name,
    val createdAt: String = "",
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val reviewNote: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "uid" to uid,
        "bdpId" to bdpId,
        "username" to username,
        "paymentMethod" to paymentMethod,
        "packageId" to packageId,
        "coinAmount" to coinAmount,
        "amount" to amount,
        "senderNumber" to senderNumber,
        "transactionId" to transactionId,
        "screenshotUrl" to screenshotUrl,
        "status" to status,
        "createdAt" to createdAt,
        "reviewedAt" to reviewedAt,
        "reviewedBy" to reviewedBy,
        "reviewNote" to reviewNote
    )
}

data class FirestoreCoinPackage(
    val packageId: String = "",
    val name: String = "",
    val coinAmount: Long = 0L,
    val price: String = "",
    val bonusCoins: Long = 0L,
    val enabled: Boolean = true,
    val sortOrder: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "packageId" to packageId,
        "name" to name,
        "coinAmount" to coinAmount,
        "price" to price,
        "bonusCoins" to bonusCoins,
        "enabled" to enabled,
        "sortOrder" to sortOrder
    )
}
