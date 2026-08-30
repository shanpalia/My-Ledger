package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    fun formatInr(amount: Double, includeSymbol: Boolean = true): String {
        val isNegative = amount < 0
        val positiveVal = abs(amount)

        // Custom Indian Numbering System formatting (e.g., 1,00,000.00)
        val formatted = try {
            val symbols = DecimalFormatSymbols(Locale("en", "IN"))
            symbols.decimalSeparator = '.'
            symbols.groupingSeparator = ','

            // Handle Indian commas: last 3 digits, then groups of 2 digits
            val integerPart = positiveVal.toLong()
            val decimalPart = String.format(Locale.US, "%.2f", positiveVal - integerPart).substring(1)

            val strInt = integerPart.toString()
            val result = StringBuilder()
            val len = strInt.length

            if (len <= 3) {
                result.append(strInt)
            } else {
                val lastThree = strInt.substring(len - 3)
                val rest = strInt.substring(0, len - 3)
                val restFormatted = StringBuilder()
                var count = 0
                for (i in rest.length - 1 downTo 0) {
                    restFormatted.insert(0, rest[i])
                    count++
                    if (count % 2 == 0 && i > 0) {
                        restFormatted.insert(0, ',')
                    }
                }
                result.append(restFormatted).append(',').append(lastThree)
            }

            if (decimalPart == ".00") {
                result.toString()
            } else {
                result.toString() + decimalPart
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", positiveVal)
        }

        val prefix = if (isNegative) "-" else ""
        val symbol = if (includeSymbol) "₹" else ""
        return "$prefix$symbol$formatted"
    }

    fun formatCompact(amount: Double): String {
        val absAmount = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absAmount >= 10000000 -> "${sign}₹${String.format(Locale.US, "%.2f", absAmount / 10000000)} Cr"
            absAmount >= 100000 -> "${sign}₹${String.format(Locale.US, "%.2f", absAmount / 100000)} L"
            absAmount >= 1000 -> "${sign}₹${String.format(Locale.US, "%.1f", absAmount / 1000)} K"
            else -> formatInr(amount)
        }
    }
}
