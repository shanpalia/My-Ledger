package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.util.PdfExporter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: LedgerViewModel,
    onNavigateToCustomerLedger: (String) -> Unit
) {
    val context = LocalContext.current
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val summaries by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.ALL_TIME) }
    var selectedCustomerId by remember { mutableStateOf("") }

    val filtered = remember(transactions, selectedPeriod, selectedCustomerId) {
        val now = System.currentTimeMillis()
        val periodStart = when (selectedPeriod) {
            ReportPeriod.DAILY -> DateUtils.getStartOfDay(now)
            ReportPeriod.WEEKLY -> DateUtils.getStartOfWeek()
            ReportPeriod.MONTHLY -> DateUtils.getStartOfMonth()
            else -> Long.MIN_VALUE
        }
        transactions
            .filter { it.transactionDate >= periodStart }
            .filter { selectedCustomerId.isBlank() || it.customerId == selectedCustomerId }
            .sortedByDescending { it.transactionDate }
    }
    val totalDebit = filtered.filter { it.transactionType == TransactionType.DEBIT.name }.sumOf { it.amount }
    val totalCredit = filtered.filter { it.transactionType == TransactionType.CREDIT.name }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tally Style Reports", fontWeight = FontWeight.Bold, color = Slate900)
                        Text(activeBiz?.businessName ?: "MY LEDGER by shanpalia", fontSize = 12.sp, color = Slate500)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val id = selectedCustomerId.ifBlank { customers.firstOrNull()?.id ?: "" }
                        if (id.isNotBlank()) viewModel.generateCustomerPdf(id) { PdfExporter.sharePdf(context, it) }
                    }) { Icon(Icons.Outlined.Share, contentDescription = "Share PDF") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(ReportPeriod.DAILY to "Day", ReportPeriod.WEEKLY to "Week", ReportPeriod.MONTHLY to "Month", ReportPeriod.ALL_TIME to "All").forEach { (period, label) ->
                                FilterChip(selected = selectedPeriod == period, onClick = { selectedPeriod = period }, label = { Text(label) }, modifier = Modifier.weight(1f))
                            }
                        }
                        val scroll = rememberScrollState()
                        Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = selectedCustomerId.isBlank(), onClick = { selectedCustomerId = "" }, label = { Text("All Customers") })
                            customers.forEach { c ->
                                FilterChip(selected = selectedCustomerId == c.id, onClick = { selectedCustomerId = c.id }, label = { Text(c.name) })
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ACCOUNT SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Debit", color = Slate500); Text(CurrencyFormatter.formatInr(totalDebit), fontWeight = FontWeight.Bold, color = DebitRed) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Credit", color = Slate500); Text(CurrencyFormatter.formatInr(totalCredit), fontWeight = FontWeight.Bold, color = CreditGreen) }
                            Column(horizontalAlignment = Alignment.End) { Text("Balance", color = Slate500); Text(CurrencyFormatter.formatInr(abs(totalDebit-totalCredit)), fontWeight = FontWeight.Bold, color = Slate900) }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Slate800)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Date", modifier = Modifier.weight(.85f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Customer / Particulars", modifier = Modifier.weight(1.8f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Debit", modifier = Modifier.weight(.75f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Credit", modifier = Modifier.weight(.75f), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (filtered.isEmpty()) {
                item { Text("No transactions for this selection.", color = Slate500, modifier = Modifier.padding(12.dp)) }
            } else {
                items(filtered, key = { it.id }) { tx ->
                    val customer = customers.find { it.id == tx.customerId }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToCustomerLedger(tx.customerId) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(DateUtils.formatDate(tx.transactionDate), modifier = Modifier.weight(.85f), fontSize = 10.sp, color = Slate600)
                            Column(Modifier.weight(1.8f)) {
                                Text(customer?.name ?: "Customer", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tx.description.ifBlank { tx.paymentMode }, fontSize = 10.sp, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(if (tx.transactionType == TransactionType.DEBIT.name) CurrencyFormatter.formatInr(tx.amount) else "-", modifier = Modifier.weight(.75f), fontSize = 11.sp, color = DebitRed)
                            Text(if (tx.transactionType == TransactionType.CREDIT.name) CurrencyFormatter.formatInr(tx.amount) else "-", modifier = Modifier.weight(.75f), fontSize = 11.sp, color = CreditGreen)
                        }
                    }
                }
            }

            if (selectedCustomerId.isBlank() && customers.isNotEmpty()) {
                item {
                    Text("CUSTOMER BALANCES", fontWeight = FontWeight.Bold, color = Slate900)
                }
                items(customers, key = { "balance_" + it.id }) { customer ->
                    val balance = summaries[customer.id]?.netBalance ?: 0.0
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth().clickable { onNavigateToCustomerLedger(customer.id) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(customer.name, fontWeight = FontWeight.Bold)
                            Text(CurrencyFormatter.formatInr(abs(balance)), fontWeight = FontWeight.Bold, color = if (balance > 0) DebitRed else if (balance < 0) CreditGreen else Slate500)
                        }
                    }
                }
            }
        }
    }
}
