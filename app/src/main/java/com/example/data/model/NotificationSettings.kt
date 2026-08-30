package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSettings(
    @PrimaryKey val id: String,
    val businessId: String,
    val pushEnabled: Boolean = true,
    val whatsappEnabled: Boolean = true,
    val smsEnabled: Boolean = true,
    val debitTemplate: String = "{shop_name}\nDear {customer_name},\n₹{amount} debit entry has been added.\nCurrent Balance: ₹{balance}\nThank You\n{shop_name}",
    val creditTemplate: String = "{shop_name}\nDear {customer_name},\n₹{amount} payment has been received.\nCurrent Balance: ₹{balance}\nThank You\n{shop_name}",
    val reminderTemplate: String = "🏪 {shop_name}\nDear {customer_name},\nYour pending balance is ₹{balance}.\nPlease clear the payment at your convenience.\nThank You\n{shop_name}",
    val createdAt: Long = System.currentTimeMillis()
)
