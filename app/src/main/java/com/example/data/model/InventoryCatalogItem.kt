package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_catalog_items")
data class InventoryCatalogItem(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val unit: String = "Pc",
    val defaultRate: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
