package com.example.ui.screens.reports

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val customerSummaries by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, DEBIT, CREDIT

    // Filter transactions based on period
    val now = System.currentTimeMillis()
    val periodFilteredTransactions = remember(allTransactions, selectedPeriod) {
        when (selectedPeriod) {
            ReportPeriod.DAILY -> {
                val start = DateUtils.getStartOfDay(now)
                allTransactions.filter { it.transactionDate >= start }
            }
            ReportPeriod.WEEKLY -> {
                val start = DateUtils.getStartOfWeek()
                allTransactions.filter { it.transactionDate >= start }
            }
            ReportPeriod.MONTHLY -> {
                val start = DateUtils.getStartOfMonth()
                allTransactions.filter { it.transactionDate >= start }
            }
            ReportPeriod.ALL_TIME, ReportPeriod.CUSTOM -> allTransactions
        }
    }

    val finalTransactions = remember(periodFilteredTransactions, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "DEBIT" -> periodFilteredTransactions.filter { it.transactionType == TransactionType.DEBIT.name }
            "CREDIT" -> periodFilteredTransactions.filter { it.transactionType == TransactionType.CREDIT.name }
            else -> periodFilteredTransactions
        }
    }

    val periodDebit = periodFilteredTransactions.filter { it.transactionType == TransactionType.DEBIT.name }.sumOf { it.amount }
    val periodCredit = periodFilteredTransactions.filter { it.transactionType == TransactionType.CREDIT.name }.sumOf { it.amount }
    val periodNet = periodDebit - periodCredit

    // Payment Mode Breakdown
    val paymentModeTotals = remember(periodFilteredTransactions) {
        periodFilteredTransactions.groupBy { it.paymentMode }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Business Reports", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate900)
                        Text(activeBiz?.businessName ?: "My Ledger", fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val firstCust = customers.firstOrNull()
                            if (firstCust != null) {
                                viewModel.generateCustomerPdf(firstCust.id) { pdf ->
                                    PdfExporter.sharePdf(context, pdf)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Slate700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector Tabs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            ReportPeriod.DAILY to "Daily",
                            ReportPeriod.WEEKLY to "Weekly",
                            ReportPeriod.MONTHLY to "Monthly",
                            ReportPeriod.ALL_TIME to "All Time"
                        ).forEach { (period, title) ->
                            val isSelected = selectedPeriod == period
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPeriod = period },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Indigo600 else Color.Transparent
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Slate600,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Summary Totals Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("report_summary_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Indigo700),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedPeriod.name.replace("_", " ")} NET CHANGE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo200,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(abs(periodNet)),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.8).sp,
                                    color = if (periodNet >= 0) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Indigo800
                            ) {
                                Text(
                                    text = "${periodFilteredTransactions.size} Transactions",
                                    color = Indigo100,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Indigo500.copy(alpha = 0.4f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TOTAL DEBIT (GIVEN)", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Indigo200)
                                Text(
                                    text = CurrencyFormatter.formatInr(periodDebit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TOTAL CREDIT (RECEIVED)", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Indigo200)
                                Text(
                                    text = CurrencyFormatter.formatInr(periodCredit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                    }
                }
            }

            // Payment Mode Distribution
            if (paymentModeTotals.isNotEmpty()) {
                item {
                    Text(
                        text = "Payment Mode Distribution",
                        fontSize = 15.sp,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(paymentModeTotals.entries.toList()) { (mode, totalAmt) ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = mode, fontSize = 11.5.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = CurrencyFormatter.formatInr(totalAmt),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo600
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Outstanding Customers in this shop
            val topDebtors = customerSummaries.values
                .filter { it.netBalance > 0 }
                .sortedByDescending { it.netBalance }

            if (topDebtors.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Outstanding Due",
                            fontSize = 15.sp,
                            color = Slate900,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Due: ${CurrencyFormatter.formatInr(topDebtors.sumOf { it.netBalance })}",
                            fontSize = 12.sp,
                            color = DebitRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(topDebtors.take(4)) { summary ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCustomerLedger(summary.customer.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = summary.customer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = summary.customer.mobile.ifEmpty { "Customer" },
                                    fontSize = 11.5.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.formatInr(summary.netBalance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DebitRed
                                )
                                Text("Pending Due", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Filter Tabs for Detailed Transactions Table
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions Log (${finalTransactions.size})",
                        fontSize = 15.sp,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedTypeFilter == "ALL",
                            onClick = { selectedTypeFilter = "ALL" },
                            label = { Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "DEBIT",
                            onClick = { selectedTypeFilter = "DEBIT" },
                            label = { Text("Debit", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "CREDIT",
                            onClick = { selectedTypeFilter = "CREDIT" },
                            label = { Text("Credit", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            if (finalTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found for this period",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(finalTransactions.take(30), key = { it.id }) { tx ->
                    val cust = customers.find { it.id == tx.customerId }
                    val isDebit = tx.transactionType == TransactionType.DEBIT.name

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = cust?.name ?: "Customer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = tx.description.ifEmpty { "Mode: ${tx.paymentMode}" },
                                    fontSize = 11.5.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = DateUtils.formatDateTime(tx.transactionDate),
                                    fontSize = 10.sp,
                                    color = Slate400Text
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isDebit) "+" else "-"}${CurrencyFormatter.formatInr(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDebit) DebitRed else CreditGreen
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDebit) DebitRedContainer else CreditGreenContainer
                                ) {
                                    Text(
                                        text = if (isDebit) "DEBIT" else "CREDIT",
                                        color = if (isDebit) DebitRed else CreditGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
