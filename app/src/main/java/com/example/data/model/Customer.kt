package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BalanceType {
    DEBIT,  // Customer owes business (You'll get)
    CREDIT  // Business owes customer (You'll give) / Advance received
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val mobile: String,
    val alternateMobile: String = "",
    val address: String = "",
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = BalanceType.DEBIT.name,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
