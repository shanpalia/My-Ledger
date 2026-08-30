package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    DEBIT,  // Amount Added / You Gave / Customer Debit (increases amount customer owes)
    CREDIT  // Payment Received / You Got / Customer Credit (reduces customer debt or adds advance)
}

enum class PaymentMode(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    BANK_TRANSFER("Bank Transfer"),
    CHEQUE("Cheque"),
    OTHER("Other")
}

@Entity(tableName = "transactions")
data class LedgerTransaction(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val transactionType: String, // "DEBIT" or "CREDIT"
    val amount: Double,
    val description: String = "",
    val paymentMode: String = PaymentMode.CASH.displayName,
    val referenceNumber: String = "",
    val attachmentUrl: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
