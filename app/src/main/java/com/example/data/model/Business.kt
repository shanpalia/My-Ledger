package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey val id: String,
    val ownerId: String,
    val businessName: String,
    val businessCategory: String = "",
    /** User-managed shop categories for this business, stored as comma-separated values. */
    val customCategoriesCsv: String = "",
    /** User-managed inventory units for this business, stored as comma-separated values. */
    val customUnitsCsv: String = "",
    val logoUrl: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val pinCode: String = "",
    val mobile: String = "",
    val email: String = "",
    val gstNumber: String = "",
    val upiId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
