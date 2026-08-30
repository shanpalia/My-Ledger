package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationChannel {
    WHATSAPP,
    SMS,
    PUSH
}

enum class NotificationStatus {
    SENT,
    FAILED,
    PENDING
}

@Entity(tableName = "notifications")
data class NotificationRecord(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val transactionId: String = "",
    val channel: String, // "WHATSAPP", "SMS", "PUSH"
    val title: String,
    val message: String,
    val status: String = NotificationStatus.SENT.name,
    val sentAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
