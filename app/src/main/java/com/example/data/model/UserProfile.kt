package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class UserProfile(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val mobile: String,
    val passwordHash: String = "",
    val pinLock: String? = null,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
