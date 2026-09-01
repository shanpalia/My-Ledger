package com.example.ui.screens.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.ReportPeriod
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.InventoryParser
import com.example.util.PdfExporter
import java.util.Calendar
import kotlin.math.abs

private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
private fun endOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
}.timeInMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: LedgerViewModel, onNavigateToCustomerLedger: (String) -> Unit) {
    val context = LocalContext.current
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.ALL_TIME) }
    var selectedCustomerId by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }

    fun pickDate(current: Long?, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current ?: System.currentTimeMillis() }
        DatePickerDialog(context, { _, y, m, d ->
            val chosen = Calendar.getInstance().apply { set(y,m,d,0,0,0); set(Calendar.MILLISECOND,0) }.timeInMillis
            selectedPeriod = ReportPeriod.CUSTOM
            onPicked(chosen)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val filtered = remember(transactions, selectedPeriod, selectedCustomerId, fromDate, toDate) {
        val now = System.currentTimeMillis()
        val range = when (selectedPeriod) {
            ReportPeriod.DAILY -> startOfDay(now) to endOfDay(now)
            ReportPeriod.WEEKLY -> DateUtils.getStartOfWeek() to now
            ReportPeriod.MONTHLY -> DateUtils.getStartOfMonth() to now
            ReportPeriod.CUSTOM -> (fromDate?.let(::startOfDay) ?: Long.MIN_VALUE) to (toDate?.let(::endOfDay) ?: Long.MAX_VALUE)
            else -> Long.MIN_VALUE to Long.MAX_VALUE
        }
        transactions.filter { it.transactionDate in range.first..range.second }
            .filter { selectedCustomerId.isBlank() || it.customerId == selectedCustomerId }
            .sortedByDescending { it.transactionDate }
    }
    val totalDebit = filtered.filter { it.transactionType == TransactionType.DEBIT.name }.sumOf { it.amount }
    val totalCredit = filtered.filter { it.transactionType == TransactionType.CREDIT.name }.sumOf { it.amount }
    val itemSummary = remember(filtered) {
        filtered.flatMap { InventoryParser.parse(it.description) }
            .groupBy { it.name.trim().lowercase() + "|" + it.unit.trim().lowercase() }
            .values.map { lines ->
                val first = lines.first(); Triple("${first.name} (${first.unit})", lines.sumOf { it.quantity }, lines.sumOf { it.amount })
            }.sortedBy { it.first.lowercase() }
    }
    val dateLabel = when (selectedPeriod) {
        ReportPeriod.CUSTOM -> "${fromDate?.let { DateUtils.formatDate(it) } ?: "Start"} - ${toDate?.let { DateUtils.formatDate(it) } ?: "End"}"
        ReportPeriod.DAILY -> DateUtils.formatDate(System.currentTimeMillis())
        ReportPeriod.WEEKLY -> "This Week"
        ReportPeriod.MONTHLY -> "This Month"
        else -> "All Dates"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Tally Style Reports", fontWeight = FontWeight.Bold, color = Slate900); Text(activeBiz?.businessName ?: "MY LEDGER", fontSize = 12.sp, color = Slate500) } },
                actions = {
                    IconButton(onClick = {
                        viewModel.generateBusinessReportPdf(filtered, customers, "TALLY STYLE REPORT", dateLabel) { PdfExporter.sharePdf(context, it) }
                    }) { Icon(Icons.Outlined.Share, contentDescription = "Share PDF") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Report Date", fontWeight = FontWeight.Bold, color = Slate900)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickDate(fromDate) { fromDate = it } }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.DateRange, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(fromDate?.let { DateUtils.formatDate(it) } ?: "From Date", maxLines = 1)
                            }
                            OutlinedButton(onClick = { pickDate(toDate) { toDate = it } }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.DateRange, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(toDate?.let { DateUtils.formatDate(it) } ?: "To Date", maxLines = 1)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(ReportPeriod.DAILY to "Day", ReportPeriod.WEEKLY to "Week", ReportPeriod.MONTHLY to "Month", ReportPeriod.ALL_TIME to "All").forEach { (period,label) ->
                                FilterChip(selected = selectedPeriod == period, onClick = { selectedPeriod = period }, label = { Text(label) }, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = selectedCustomerId.isBlank(), onClick = { selectedCustomerId = "" }, label = { Text("All Customers") })
                            customers.forEach { c -> FilterChip(selected = selectedCustomerId == c.id, onClick = { selectedCustomerId = c.id }, label = { Text(c.name) }) }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("DATE: $dateLabel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Debit", color = Slate500); Text(CurrencyFormatter.formatInr(totalDebit), fontWeight = FontWeight.Bold, color = DebitRed) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Credit", color = Slate500); Text(CurrencyFormatter.formatInr(totalCredit), fontWeight = FontWeight.Bold, color = CreditGreen) }
                            Column(horizontalAlignment = Alignment.End) { Text("Net Due", color = Slate500); Text(CurrencyFormatter.formatInr(abs(totalDebit-totalCredit)), fontWeight = FontWeight.Bold, color = Slate900) }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DATE-WISE ITEM & PAYMENT REPORT", fontWeight = FontWeight.Bold, color = Slate900)
                        Text("Items are shown exactly as entered. Payments reduce the running balance.", fontSize = 11.sp, color = Slate500)
                        var runningBalance = 0.0
                        val chronological = filtered.sortedBy { it.transactionDate }
                        if (chronological.isEmpty()) {
                            Text("No transactions for this selection.", color = Slate500, fontSize = 12.sp)
                        } else {
                            chronological.groupBy { DateUtils.formatDate(it.transactionDate) }.forEach { (date, dayTransactions) ->
                                Surface(shape = RoundedCornerShape(10.dp), color = Slate800, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(date, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Opening: ${CurrencyFormatter.formatInr(abs(runningBalance))}${if (runningBalance > 0.0001) " DR" else if (runningBalance < -0.0001) " CR" else ""}", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                                dayTransactions.forEach { tx ->
                                    val customerName = customers.find { it.id == tx.customerId }?.name ?: "Customer"
                                    val inv = InventoryParser.parse(tx.description)
                                    Text(customerName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Slate700)
                                    if (inv.isNotEmpty()) {
                                        inv.forEach { line ->
                                            Text("${line.name} ${line.quantity} ${line.unit} × ${line.rate} = ${CurrencyFormatter.formatInr(line.amount)}", fontSize = 13.sp, color = Slate900, modifier = Modifier.padding(start = 8.dp))
                                        }
                                        runningBalance += tx.amount
                                        Text("Total = ${CurrencyFormatter.formatInr(tx.amount)}", fontWeight = FontWeight.Bold, color = Indigo600, modifier = Modifier.fillMaxWidth())
                                    } else {
                                        val label = if (tx.transactionType == TransactionType.CREDIT.name) "Received Payment" else "Debit Entry"
                                        Text("$label = ${CurrencyFormatter.formatInr(tx.amount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (tx.transactionType == TransactionType.CREDIT.name) CreditGreen else DebitRed, modifier = Modifier.padding(start = 8.dp))
                                        runningBalance += if (tx.transactionType == TransactionType.CREDIT.name) -tx.amount else tx.amount
                                    }
                                    Text("Running Balance = ${CurrencyFormatter.formatInr(abs(runningBalance))}${if (runningBalance > 0.0001) " DR" else if (runningBalance < -0.0001) " CR" else ""}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Slate700, modifier = Modifier.fillMaxWidth())
                                    HorizontalDivider(color = BorderSlate100)
                                }
                            }
                        }
                        HorizontalDivider(color = BorderSlate100)
                        Text("FINAL BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text("${CurrencyFormatter.formatInr(abs(runningBalance))}${if (runningBalance > 0.0001) " DR" else if (runningBalance < -0.0001) " CR" else ""}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (runningBalance >= 0) DebitRed else CreditGreen)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Slate800)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Date", modifier = Modifier.weight(.8f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Customer / Item", modifier = Modifier.weight(2f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Amount", modifier = Modifier.weight(.8f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (filtered.isEmpty()) item { Text("No transactions for this selection.", color = Slate500, modifier = Modifier.padding(12.dp)) }
            else items(filtered, key = { it.id }) { tx ->
                val customer = customers.find { it.id == tx.customerId }
                val inv = InventoryParser.parse(tx.description)
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { onNavigateToCustomerLedger(tx.customerId) }) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(DateUtils.formatDate(tx.transactionDate), color = Slate500, fontSize = 11.sp)
                            Text(customer?.name ?: "Customer", fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        if (inv.isNotEmpty()) inv.forEach { item -> Text("${item.name} | ${item.quantity} ${item.unit} × ${item.rate} = ${CurrencyFormatter.formatInr(item.amount)}", fontSize = 12.sp, color = Slate700) }
                        else Text(tx.description.ifBlank { tx.paymentMode }, fontSize = 12.sp, color = Slate600, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(CurrencyFormatter.formatInr(tx.amount), fontWeight = FontWeight.Bold, color = if (tx.transactionType == TransactionType.DEBIT.name) DebitRed else CreditGreen)
                    }
                }
            }
        }
    }
}
