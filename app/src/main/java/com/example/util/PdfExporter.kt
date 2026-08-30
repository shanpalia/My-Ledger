package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Business
import com.example.data.model.Customer
import com.example.data.model.LedgerTransaction
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

data class LedgerItemWithBalance(
    val transaction: LedgerTransaction,
    val debitAmount: Double,
    val creditAmount: Double,
    val runningBalance: Double
)

object PdfExporter {

    fun generateLedgerPdf(
        context: Context,
        business: Business,
        customer: Customer,
        items: List<LedgerItemWithBalance>,
        totalDebit: Double,
        totalCredit: Double,
        netBalance: Double
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard point width
            val pageHeight = 842 // A4 standard point height
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Header Background Accent
            paint.color = Color.rgb(15, 23, 42) // Deep Slate
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

            // Shop Name
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(business.businessName.ifEmpty { "My Ledger" }, 30f, 40f, paint)

            // Shop Address & Mobile
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val addressText = listOfNotNull(
                business.address.takeIf { it.isNotEmpty() },
                business.city.takeIf { it.isNotEmpty() },
                business.state.takeIf { it.isNotEmpty() },
                business.pinCode.takeIf { it.isNotEmpty() }
            ).joinToString(", ")
            if (addressText.isNotEmpty()) {
                canvas.drawText(addressText, 30f, 58f, paint)
            }
            val contactInfo = listOfNotNull(
                business.mobile.takeIf { it.isNotEmpty() }?.let { "Mobile: $it" },
                business.gstNumber.takeIf { it.isNotEmpty() }?.let { "GST: $it" },
                business.upiId.takeIf { it.isNotEmpty() }?.let { "UPI: $it" }
            ).joinToString(" | ")
            canvas.drawText(contactInfo.ifEmpty { "Smart Business Ledger Statement" }, 30f, 74f, paint)

            // Title badge on top right
            paint.color = Color.rgb(30, 64, 175)
            canvas.drawRoundRect(pageWidth - 170f, 25f, pageWidth - 30f, 65f, 8f, 8f, paint)
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ACCOUNT STATEMENT", pageWidth - 160f, 48f, paint)

            var currentY = 125f

            // Customer Details Card
            paint.color = Color.rgb(241, 245, 249) // Light grey background
            canvas.drawRoundRect(30f, currentY, pageWidth - 30f, currentY + 65f, 6f, 6f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Customer: ${customer.name}", 45f, currentY + 24f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Mobile: ${customer.mobile}", 45f, currentY + 42f, paint)
            if (customer.address.isNotEmpty()) {
                canvas.drawText("Address: ${customer.address}", 45f, currentY + 56f, paint)
            }

            // Statement Date
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Date: ${DateUtils.formatDate(System.currentTimeMillis())}", pageWidth - 45f, currentY + 24f, paint)
            val balanceLabel = if (netBalance >= 0) "Net Due (Debit)" else "Advance (Credit)"
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (netBalance >= 0) Color.rgb(220, 38, 38) else Color.rgb(5, 150, 105)
            canvas.drawText("$balanceLabel: ${CurrencyFormatter.formatInr(netBalance)}", pageWidth - 45f, currentY + 48f, paint)
            paint.textAlign = Paint.Align.LEFT

            currentY += 85f

            // Table Header
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 24f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Date", 40f, currentY + 16f, paint)
            canvas.drawText("Description / Mode", 120f, currentY + 16f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Debit (₹)", 370f, currentY + 16f, paint)
            canvas.drawText("Credit (₹)", 460f, currentY + 16f, paint)
            canvas.drawText("Balance (₹)", pageWidth - 40f, currentY + 16f, paint)
            paint.textAlign = Paint.Align.LEFT

            currentY += 24f

            // Table Rows
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val rowHeight = 22f

            items.take(24).forEachIndexed { index, item ->
                if (index % 2 == 0) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + rowHeight, paint)
                }

                // Date
                paint.color = Color.rgb(51, 65, 85)
                paint.textSize = 9f
                canvas.drawText(DateUtils.formatDate(item.transaction.transactionDate), 40f, currentY + 15f, paint)

                // Description
                val desc = item.transaction.description.ifEmpty {
                    if (item.transaction.transactionType == TransactionType.DEBIT.name) "Debit Entry" else "Payment Received (${item.transaction.paymentMode})"
                }
                val trimmedDesc = if (desc.length > 32) desc.substring(0, 30) + "..." else desc
                canvas.drawText(trimmedDesc, 120f, currentY + 15f, paint)

                paint.textAlign = Paint.Align.RIGHT

                // Debit
                if (item.debitAmount > 0) {
                    paint.color = Color.rgb(220, 38, 38)
                    canvas.drawText(CurrencyFormatter.formatInr(item.debitAmount, includeSymbol = false), 370f, currentY + 15f, paint)
                } else {
                    paint.color = Color.rgb(148, 163, 184)
                    canvas.drawText("-", 370f, currentY + 15f, paint)
                }

                // Credit
                if (item.creditAmount > 0) {
                    paint.color = Color.rgb(5, 150, 105)
                    canvas.drawText(CurrencyFormatter.formatInr(item.creditAmount, includeSymbol = false), 460f, currentY + 15f, paint)
                } else {
                    paint.color = Color.rgb(148, 163, 184)
                    canvas.drawText("-", 460f, currentY + 15f, paint)
                }

                // Balance
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(CurrencyFormatter.formatInr(item.runningBalance, includeSymbol = false), pageWidth - 40f, currentY + 15f, paint)

                paint.textAlign = Paint.Align.LEFT
                currentY += rowHeight
            }

            // Summary Bottom Card
            currentY += 15f
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(30f, currentY, pageWidth - 30f, currentY + 50f, 6f, 6f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Total Debit: ${CurrencyFormatter.formatInr(totalDebit)}", 45f, currentY + 22f, paint)
            canvas.drawText("Total Credit: ${CurrencyFormatter.formatInr(totalCredit)}", 220f, currentY + 22f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Final Balance: ${CurrencyFormatter.formatInr(netBalance)}", pageWidth - 45f, currentY + 22f, paint)
            paint.textAlign = Paint.Align.LEFT

            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("Please verify all transactions. Contact shop for discrepancies.", 45f, currentY + 40f, paint)

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            paint.strokeWidth = 0.8f
            canvas.drawLine(30f, pageHeight - 45f, pageWidth - 30f, pageHeight - 45f, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("Generated by My Ledger • Smart Debit & Credit Management", 30f, pageHeight - 30f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page 1 of 1", pageWidth - 30f, pageHeight - 30f, paint)

            pdfDocument.finishPage(page)

            val dir = File(context.cacheDir, "ledgers").apply { mkdirs() }
            val sanitizedName = customer.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val file = File(dir, "Ledger_${sanitizedName}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Ledger PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String = "Ledger_Print") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val pdi = PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(pdi, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        var inStream: InputStream? = null
                        var outStream: OutputStream? = null
                        try {
                            inStream = FileInputStream(pdfFile)
                            outStream = FileOutputStream(destination?.fileDescriptor)
                            val buf = ByteArray(1024)
                            var bytesRead: Int
                            while (inStream.read(buf).also { bytesRead = it } > 0) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onWriteCancelled()
                                    return
                                }
                                outStream.write(buf, 0, bytesRead)
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        } finally {
                            try { inStream?.close() } catch (ignored: Exception) {}
                            try { outStream?.close() } catch (ignored: Exception) {}
                        }
                    }
                }
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
