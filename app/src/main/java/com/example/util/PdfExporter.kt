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
import com.example.util.InventoryParser
import java.util.Date
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
            val doc = PdfDocument()
            val width = 595
            val height = 842
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 0f
            val paint = Paint().apply { isAntiAlias = true }

            fun text(value: String, x: Float, yy: Float, size: Float = 10f, color: Int = Color.rgb(30, 41, 59), bold: Boolean = false) {
                paint.color = color
                paint.textSize = size
                paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                paint.textAlign = Paint.Align.LEFT
                canvas!!.drawText(value, x, yy, paint)
            }
            fun newPage() {
                page?.let { doc.finishPage(it) }
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
                canvas = page!!.canvas
                paint.color = Color.rgb(15, 23, 42)
                canvas!!.drawRect(0f, 0f, width.toFloat(), 92f, paint)
                text(business.businessName.ifBlank { "MY LEDGER" }, 30f, 38f, 20f, Color.WHITE, true)
                text("ACCOUNT STATEMENT • ITEM-WISE REPORT", 30f, 60f, 10f, Color.WHITE, true)
                text("Customer: ${customer.name}", 30f, 78f, 10f, Color.WHITE)
                y = 112f
            }
            fun ensure(space: Float) { if (y + space > height - 55f) newPage() }

            newPage()
            text("CUSTOMER: ${customer.name}", 30f, y, 14f, bold = true); y += 20f
            text("Mobile: ${customer.mobile.ifBlank { "-" }}", 30f, y, 10f); y += 18f
            text("FINAL BALANCE: ${CurrencyFormatter.formatInr(netBalance)}", 30f, y, 12f, if (netBalance >= 0) Color.rgb(220,38,38) else Color.rgb(5,150,105), true); y += 26f

            val inventoryItems = items.filter { InventoryParser.parse(it.transaction.description).isNotEmpty() }
            text("ITEM-WISE ENTRIES (AS ENTERED)", 30f, y, 13f, Color.rgb(30,64,175), true); y += 20f
            if (inventoryItems.isEmpty()) {
                text("No inventory items found.", 30f, y, 10f, Color.rgb(100,116,139)); y += 20f
            } else {
                inventoryItems.sortedBy { it.transaction.transactionDate }.forEach { entry ->
                    ensure(34f)
                    text("${DateUtils.formatDate(entry.transaction.transactionDate)}", 30f, y, 10f, Color.rgb(71,85,105), true); y += 16f
                    InventoryParser.parse(entry.transaction.description).forEach { line ->
                        ensure(18f)
                        text("${line.name} ${line.quantity} ${line.unit} × ${line.rate} = ${CurrencyFormatter.formatInr(line.amount)}", 42f, y, 10f)
                        y += 16f
                    }
                    ensure(18f)
                    text("Total = ${CurrencyFormatter.formatInr(entry.transaction.amount)}", 42f, y, 10f, Color.rgb(30,64,175), true)
                    y += 22f
                }
            }

            ensure(34f)
            text("TRANSACTION SUMMARY", 30f, y, 13f, Color.rgb(30,64,175), true); y += 20f
            items.sortedBy { it.transaction.transactionDate }.forEach { entry ->
                ensure(20f)
                val tx = entry.transaction
                val kind = if (tx.transactionType == TransactionType.DEBIT.name) "Debit" else "Credit"
                text("${DateUtils.formatDate(tx.transactionDate)} • $kind • ${CurrencyFormatter.formatInr(tx.amount)} • Balance ${CurrencyFormatter.formatInr(entry.runningBalance)}", 30f, y, 9.5f)
                y += 17f
            }

            ensure(46f)
            paint.color = Color.rgb(241,245,249)
            canvas!!.drawRoundRect(30f, y, width - 30f, y + 44f, 6f, 6f, paint)
            text("Total Debit: ${CurrencyFormatter.formatInr(totalDebit)}", 45f, y + 20f, 10f, bold = true)
            text("Total Credit: ${CurrencyFormatter.formatInr(totalCredit)}", 220f, y + 20f, 10f, bold = true)
            text("Final Balance: ${CurrencyFormatter.formatInr(netBalance)}", 385f, y + 20f, 10f, bold = true)

            page?.let { doc.finishPage(it) }
            val dir = File(context.cacheDir, "ledgers").apply { mkdirs() }
            val sanitizedName = customer.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val file = File(dir, "Ledger_${sanitizedName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateBusinessReportPdf(
        context: Context,
        business: Business,
        transactions: List<LedgerTransaction>,
        customers: List<Customer>,
        reportTitle: String,
        dateLabel: String
    ): File? {
        return try {
            val doc = PdfDocument()
            val width = 595
            val height = 842
            val customerMap = customers.associateBy { it.id }
            val sorted = transactions.sortedBy { it.transactionDate }
            var runningBalance = 0.0
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 0f
            val paint = Paint().apply { isAntiAlias = true }

            fun newPage() {
                page?.let { doc.finishPage(it) }
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
                canvas = page!!.canvas
                y = 0f
                paint.color = Color.rgb(15, 23, 42)
                canvas!!.drawRect(0f, 0f, width.toFloat(), 112f, paint)
                paint.color = Color.WHITE; paint.textSize = 22f; paint.typeface = Typeface.DEFAULT_BOLD
                canvas!!.drawText(business.businessName.ifBlank { "MY LEDGER" }, 30f, 38f, paint)
                paint.textSize = 11f; paint.typeface = Typeface.DEFAULT
                val address = business.address.ifBlank { business.city }.ifBlank { "Business Ledger" }
                canvas!!.drawText(address.take(90), 30f, 58f, paint)
                val contact = listOfNotNull(
                    business.mobile.takeIf { it.isNotBlank() }?.let { "Mobile: $it" },
                    business.email.takeIf { it.isNotBlank() }
                ).joinToString("   •   ")
                if (contact.isNotBlank()) canvas!!.drawText(contact.take(90), 30f, 76f, paint)
                paint.textSize = 11f; paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.RIGHT
                canvas!!.drawText(reportTitle, width - 30f, 42f, paint)
                paint.textSize = 9.5f; paint.typeface = Typeface.DEFAULT
                canvas!!.drawText("Period: $dateLabel", width - 30f, 64f, paint)
                paint.textAlign = Paint.Align.LEFT
                y = 132f
            }
            fun ensure(space: Float) { if (y + space > height - 58f) newPage() }
            fun text(txt: String, x: Float, yy: Float, size: Float = 10f, color: Int = Color.rgb(30,41,59), bold: Boolean = false) {
                paint.color = color; paint.textSize = size; paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                canvas!!.drawText(txt, x, yy, paint)
            }
            fun amountLabel(value: Double): String = CurrencyFormatter.formatInr(kotlin.math.abs(value))
            fun balanceLabel(value: Double): String = when {
                value > 0.0001 -> "${amountLabel(value)} DR"
                value < -0.0001 -> "${amountLabel(value)} CR"
                else -> "₹0"
            }
            fun paymentLabel(tx: LedgerTransaction): String {
                val base = if (tx.transactionType == TransactionType.CREDIT.name) "Received Payment" else "Debit Entry"
                val mode = tx.paymentMode.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                val desc = tx.description.trim().takeIf { it.isNotBlank() && InventoryParser.parse(it).isEmpty() }
                return if (desc != null) "$base$mode — $desc" else "$base$mode"
            }

            newPage()
            text("DATE-WISE ITEM & PAYMENT REPORT", 30f, y, 15f, bold = true); y += 28f

            if (sorted.isEmpty()) {
                text("No transactions for the selected period.", 30f, y, 11f, Color.rgb(100,116,139)); y += 24f
            } else {
                val grouped = sorted.groupBy { DateUtils.formatDate(it.transactionDate) }
                grouped.forEach { (date, dayTransactions) ->
                    ensure(44f)
                    paint.color = Color.rgb(30,41,59)
                    canvas!!.drawRoundRect(30f, y, width - 30f, y + 30f, 6f, 6f, paint)
                    text(date, 42f, y + 20f, 12f, Color.WHITE, true)
                    paint.textAlign = Paint.Align.RIGHT
                    text("Opening: ${balanceLabel(runningBalance)}", width - 42f, y + 20f, 10f, Color.WHITE, true)
                    paint.textAlign = Paint.Align.LEFT
                    y += 42f

                    dayTransactions.forEach { tx ->
                        val customer = customerMap[tx.customerId]?.name ?: "Customer"
                        val inventory = InventoryParser.parse(tx.description)
                        ensure(if (inventory.isNotEmpty()) 28f + inventory.size * 18f else 42f)
                        text(customer, 38f, y, 10.5f, Color.rgb(51,65,85), true); y += 16f
                        if (inventory.isNotEmpty()) {
                            inventory.forEach { line ->
                                ensure(18f)
                                text("${line.name} ${line.quantity} ${line.unit} × ${line.rate} = ${CurrencyFormatter.formatInr(line.amount)}", 50f, y, 10.5f)
                                y += 16f
                            }
                            runningBalance += tx.amount
                            paint.textAlign = Paint.Align.RIGHT
                            text("= ${CurrencyFormatter.formatInr(tx.amount)}", width - 42f, y - 2f, 10.5f, Color.rgb(30,64,175), true)
                            paint.textAlign = Paint.Align.LEFT
                        } else {
                            text(paymentLabel(tx), 50f, y, 10.5f, if (tx.transactionType == TransactionType.CREDIT.name) Color.rgb(5,150,105) else Color.rgb(220,38,38), true)
                            y += 16f
                            if (tx.transactionType == TransactionType.CREDIT.name) runningBalance -= tx.amount else runningBalance += tx.amount
                            paint.textAlign = Paint.Align.RIGHT
                            text("= ${CurrencyFormatter.formatInr(tx.amount)}", width - 42f, y - 2f, 10.5f, if (tx.transactionType == TransactionType.CREDIT.name) Color.rgb(5,150,105) else Color.rgb(220,38,38), true)
                            paint.textAlign = Paint.Align.LEFT
                        }
                        ensure(20f)
                        text("Running Balance", 50f, y + 8f, 9.5f, Color.rgb(100,116,139))
                        paint.textAlign = Paint.Align.RIGHT
                        text(balanceLabel(runningBalance), width - 42f, y + 8f, 10f, Color.rgb(30,41,59), true)
                        paint.textAlign = Paint.Align.LEFT
                        y += 24f
                    }
                    y += 8f
                }
            }

            ensure(64f)
            val totalDebit = sorted.filter { it.transactionType == TransactionType.DEBIT.name }.sumOf { it.amount }
            val totalCredit = sorted.filter { it.transactionType == TransactionType.CREDIT.name }.sumOf { it.amount }
            paint.color = Color.rgb(241,245,249)
            canvas!!.drawRoundRect(30f, y, width - 30f, y + 58f, 8f, 8f, paint)
            text("Total Debit: ${CurrencyFormatter.formatInr(totalDebit)}", 45f, y + 22f, 10.5f, bold = true)
            text("Total Credit: ${CurrencyFormatter.formatInr(totalCredit)}", 45f, y + 42f, 10.5f, bold = true)
            paint.textAlign = Paint.Align.RIGHT
            text("FINAL BALANCE", width - 45f, y + 22f, 10f, Color.rgb(71,85,105), true)
            text(balanceLabel(runningBalance), width - 45f, y + 43f, 16f, if (runningBalance >= 0) Color.rgb(185,28,28) else Color.rgb(5,150,105), true)
            paint.textAlign = Paint.Align.LEFT

            val footerY = height - 28f
            paint.color = Color.rgb(148,163,184); paint.strokeWidth = 0.7f
            canvas!!.drawLine(30f, footerY - 10f, width - 30f, footerY - 10f, paint)
            text("Generated by My Ledger • Date-wise Item & Payment Statement", 30f, footerY, 8.5f, Color.rgb(100,116,139))
            paint.textAlign = Paint.Align.RIGHT
            text("Page $pageNumber", width - 30f, footerY, 8.5f, Color.rgb(100,116,139))
            paint.textAlign = Paint.Align.LEFT

            page?.let { doc.finishPage(it) }
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "MyLedger_Professional_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) { e.printStackTrace(); null }
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
