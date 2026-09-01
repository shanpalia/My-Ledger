package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import com.example.data.model.*
import java.net.URLEncoder

object NotificationHelper {

    fun buildMessage(
        template: String,
        shopName: String,
        customerName: String,
        amount: Double,
        balance: Double,
        transactionDate: Long,
        transactionType: String,
        inventoryDetails: String = ""
    ): String {
        val formattedAmount = CurrencyFormatter.formatInr(amount, includeSymbol = false)
        val formattedBalance = CurrencyFormatter.formatInr(balance, includeSymbol = false)
        val formattedDate = DateUtils.formatDate(transactionDate)
        val typeLabel = if (transactionType == TransactionType.DEBIT.name) "Debit Entry" else "Payment Received"

        return template
            .replace("{shop_name}", shopName)
            .replace("{customer_name}", customerName)
            .replace("{amount}", formattedAmount)
            .replace("{balance}", formattedBalance)
            .replace("{transaction_date}", formattedDate)
.replace("{transaction_type}", typeLabel)
            .let { base ->
                if (inventoryDetails.isBlank()) base else "$base\n\nItems / Inventory:\n$inventoryDetails"
            }
    }

    fun buildReminderMessage(
        template: String,
        shopName: String,
        customerName: String,
        pendingBalance: Double
    ): String {
        val formattedBalance = CurrencyFormatter.formatInr(pendingBalance, includeSymbol = false)
        return template
            .replace("{shop_name}", shopName)
            .replace("{customer_name}", customerName)
            .replace("{balance}", formattedBalance)
    }


    /** Opens WhatsApp with a PDF attachment and a prefilled reminder message. */
    fun sharePdfToWhatsApp(context: Context, rawMobile: String, message: String, pdfFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = android.content.ClipData.newRawUri("Ledger PDF", uri)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    clipData = android.content.ClipData.newRawUri("Ledger PDF", uri)
                }
                context.startActivity(Intent.createChooser(fallback, "Send reminder with PDF"))
                true
            } catch (inner: Exception) {
                Toast.makeText(context, "Could not share PDF: ${inner.message}", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    fun openWhatsApp(context: Context, rawMobile: String, message: String): Boolean {
        return try {
            // Clean phone number (strip spaces, dashes, ensure country code)
            var cleanPhone = rawMobile.replace(Regex("[^0-9]"), "")
            if (cleanPhone.length == 10) {
                cleanPhone = "91$cleanPhone" // Default to India (+91)
            }
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            shareTextFallback(context, message, "Send via WhatsApp")
            false
        }
    }

    fun openSms(context: Context, rawMobile: String, message: String): Boolean {
        return try {
            val uri = Uri.parse("smsto:$rawMobile")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra("sms_body", message)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            shareTextFallback(context, message, "Send SMS")
            false
        }
    }

    fun shareTextFallback(context: Context, message: String, title: String = "Share Message") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share app", Toast.LENGTH_SHORT).show()
        }
    }
}
