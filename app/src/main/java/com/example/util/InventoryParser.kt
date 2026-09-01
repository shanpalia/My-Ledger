package com.example.util

data class InventoryItemSummary(
    val name: String,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val amount: Double
)

object InventoryParser {
    private val linePattern = Regex("^\\s*(.+?)\\s*\\|\\s*([0-9]+(?:\\.[0-9]+)?)\\s+([^×=]+?)\\s*×\\s*([0-9]+(?:\\.[0-9]+)?)\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$")

    fun parse(description: String): List<InventoryItemSummary> = description
        .lineSequence()
        .mapNotNull { line ->
            val m = linePattern.matchEntire(line) ?: return@mapNotNull null
            InventoryItemSummary(
                name = m.groupValues[1].trim(),
                quantity = m.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null,
                unit = m.groupValues[3].trim(),
                rate = m.groupValues[4].toDoubleOrNull() ?: return@mapNotNull null,
                amount = m.groupValues[5].toDoubleOrNull() ?: return@mapNotNull null
            )
        }.toList()
}
