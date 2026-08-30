package com.example.ui.screens.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BalanceType
import com.example.data.model.LedgerTransaction
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.*
import kotlin.math.abs

enum class LedgerTxFilter {
    ALL,
    DEBIT_ONLY,
    CREDIT_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    customerId: String,
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (String, TransactionType) -> Unit
) {
    val context = LocalContext.current
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val ledgerItems by viewModel.selectedCustomerLedgerItems.collectAsStateWithLifecycle()
    val customerSummaries by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val notifSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()

    val customer = customers.find { it.id == customerId }
    val summary = customerSummaries[customerId]

    var txFilter by remember { mutableStateOf(LedgerTxFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showReminderDialog by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<LedgerTransaction?>(null) }

    val totalDebit = summary?.totalDebit ?: 0.0
    val totalCredit = summary?.totalCredit ?: 0.0
    val netBalance = summary?.netBalance ?: 0.0

    val filteredItems = ledgerItems.filter { item ->
        val matchesFilter = when (txFilter) {
            LedgerTxFilter.ALL -> true
            LedgerTxFilter.DEBIT_ONLY -> item.debitAmount > 0
            LedgerTxFilter.CREDIT_ONLY -> item.creditAmount > 0
        }
        val matchesSearch = searchQuery.isEmpty() ||
                item.transaction.description.contains(searchQuery, ignoreCase = true) ||
                item.transaction.paymentMode.contains(searchQuery, ignoreCase = true) ||
                item.transaction.referenceNumber.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer?.name ?: "Customer Ledger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = customer?.mobile ?: "",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // PDF Statement Button
                    IconButton(
                        onClick = {
                            viewModel.generateCustomerPdf(customerId) { pdfFile ->
                                PdfExporter.sharePdf(context, pdfFile)
                            }
                        },
                        modifier = Modifier.testTag("action_export_pdf")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF", tint = PrimaryBlueLight)
                    }
                    // Print Button
                    IconButton(
                        onClick = {
                            viewModel.generateCustomerPdf(customerId) { pdfFile ->
                                PdfExporter.printPdf(context, pdfFile, "Statement_${customer?.name}")
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = "Print Ledger", tint = Slate700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BorderSlate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onNavigateToAddTransaction(customerId, TransactionType.DEBIT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("button_add_debit_ledger"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DebitRed)
                    ) {
                        Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("You Gave (₹)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { onNavigateToAddTransaction(customerId, TransactionType.CREDIT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("button_add_credit_ledger"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditGreen)
                    ) {
                        Icon(Icons.Default.SouthWest, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("You Got (₹)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Ledger Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Indigo700),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "NET CURRENT BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo200,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(abs(netBalance)),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.8).sp,
                                    color = when {
                                        netBalance > 0 -> Color(0xFFFCA5A5) // red-300
                                        netBalance < 0 -> Color(0xFF6EE7B7) // emerald-300
                                        else -> Color.White
                                    }
                                )
                                Text(
                                    text = when {
                                        netBalance > 0 -> "Customer owes you (Debit Due)"
                                        netBalance < 0 -> "You owe customer (Advance / Credit)"
                                        else -> "Account is all settled"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            // Quick reminder button if debit balance
                            if (netBalance > 0) {
                                Button(
                                    onClick = { showReminderDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Indigo700)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reminder", color = Indigo700, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = Indigo500.copy(alpha = 0.4f), thickness = 1.dp)

                        // Debit vs Credit breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TOTAL DEBIT (+)", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Indigo200)
                                Text(
                                    text = CurrencyFormatter.formatInr(totalDebit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TOTAL CREDIT (-)", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Indigo200)
                                Text(
                                    text = CurrencyFormatter.formatInr(totalCredit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }

                        // Quick Call / WhatsApp Contact Row
                        if (customer?.mobile?.isNotEmpty() == true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val msg = "🏪 ${activeBiz?.businessName ?: "My Ledger"}\nDear ${customer.name},\nYour current balance with us is ${CurrencyFormatter.formatInr(netBalance)}.\nThank You!"
                                        NotificationHelper.openWhatsApp(context, customer.mobile, msg)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6EE7B7)),
                                    border = BorderStroke(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Filters & Search
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = txFilter == LedgerTxFilter.ALL,
                        onClick = { txFilter = LedgerTxFilter.ALL },
                        label = { Text("All (${ledgerItems.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = txFilter == LedgerTxFilter.DEBIT_ONLY,
                        onClick = { txFilter = LedgerTxFilter.DEBIT_ONLY },
                        label = { Text("Debit (+)", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = txFilter == LedgerTxFilter.CREDIT_ONLY,
                        onClick = { txFilter = LedgerTxFilter.CREDIT_ONLY },
                        label = { Text("Credit (-)", fontSize = 12.sp) }
                    )
                }
            }

            // Table Header Bar
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ENTRIES & DETAILS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("DEBIT / CREDIT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("BALANCE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Opening Balance Item
            if (customer != null && customer.openingBalance > 0) {
                item {
                    val isOpeningDebit = customer.openingBalanceType == BalanceType.DEBIT.name
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Opening Balance",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = DateUtils.formatDate(customer.createdAt),
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${if (isOpeningDebit) "+" else "-"}${CurrencyFormatter.formatInr(customer.openingBalance)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isOpeningDebit) DebitRed else CreditGreen
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(if (isOpeningDebit) customer.openingBalance else -customer.openingBalance),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                            }
                        }
                    }
                }
            }

            // Transaction Rows
            if (filteredItems.isEmpty() && (customer?.openingBalance ?: 0.0) == 0.0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ledger entries yet.\nTap 'You Gave' or 'You Got' to add.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredItems.reversed(), key = { it.transaction.id }) { item ->
                    LedgerTableRow(
                        item = item,
                        onClick = { selectedTxForDetails = item.transaction }
                    )
                }
            }
        }
    }

    // Payment Reminder Dialog
    if (showReminderDialog && customer != null) {
        val defaultTemplate = notifSettings?.reminderTemplate
            ?: "🏪 {shop_name}\nDear {customer_name},\nYour pending balance is ₹{balance}.\nPlease clear the payment at your convenience.\nThank You\n{shop_name}"

        var reminderMsg by remember {
            mutableStateOf(
                NotificationHelper.buildReminderMessage(
                    template = defaultTemplate,
                    shopName = activeBiz?.businessName ?: "My Ledger",
                    customerName = customer.name,
                    pendingBalance = netBalance
                )
            )
        }

        Dialog(onDismissRequest = { showReminderDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PendingAmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PendingAmber)
                        }
                        Column {
                            Text("Send Payment Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Customer: ${customer.name}", fontSize = 12.sp, color = Slate500)
                        }
                    }

                    OutlinedTextField(
                        value = reminderMsg,
                        onValueChange = { reminderMsg = it },
                        label = { Text("Reminder Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                NotificationHelper.openWhatsApp(context, customer.mobile, reminderMsg)
                                showReminderDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CreditGreen)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                NotificationHelper.openSms(context, customer.mobile, reminderMsg)
                                showReminderDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                NotificationHelper.shareTextFallback(context, reminderMsg, "Share Payment Reminder")
                                showReminderDialog = false
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            }
        }
    }

    // Transaction Details Dialog
    if (selectedTxForDetails != null) {
        val tx = selectedTxForDetails!!
        val isDebit = tx.transactionType == TransactionType.DEBIT.name
        Dialog(onDismissRequest = { selectedTxForDetails = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transaction Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDebit) DebitRedContainer else CreditGreenContainer
                        ) {
                            Text(
                                text = if (isDebit) "DEBIT (You Gave)" else "CREDIT (You Got)",
                                color = if (isDebit) DebitRed else CreditGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = CurrencyFormatter.formatInr(tx.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (isDebit) DebitRed else CreditGreen
                    )

                    HorizontalDivider(color = Slate100)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Date: ${DateUtils.formatDateTime(tx.transactionDate)}", fontSize = 12.5.sp, color = Slate700)
                        Text("Payment Mode: ${tx.paymentMode}", fontSize = 12.5.sp, color = Slate700)
                        if (tx.referenceNumber.isNotEmpty()) {
                            Text("Reference / Bill No: ${tx.referenceNumber}", fontSize = 12.5.sp, color = Slate700)
                        }
                        if (tx.description.isNotEmpty()) {
                            Text("Description: ${tx.description}", fontSize = 12.5.sp, color = Slate700)
                        }
                        if (tx.createdBy.isNotEmpty()) {
                            Text("Recorded by: ${tx.createdBy}", fontSize = 11.5.sp, color = Slate500)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteTransaction(tx)
                                selectedTxForDetails = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = DebitRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Entry")
                        }

                        Button(
                            onClick = { selectedTxForDetails = null },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerTableRow(
    item: LedgerItemWithBalance,
    onClick: () -> Unit
) {
    val tx = item.transaction
    val isDebit = item.debitAmount > 0
    val amountColor = if (isDebit) DebitRed else CreditGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("ledger_row_${tx.id}"),
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDebit) DebitRedContainer else CreditGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowOutward else Icons.Default.SouthWest,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = tx.description.ifEmpty { if (isDebit) "Debit Entry" else "Payment Received" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${DateUtils.formatDate(tx.transactionDate)} • ${tx.paymentMode}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${if (isDebit) "+" else "-"}${CurrencyFormatter.formatInr(if (isDebit) item.debitAmount else item.creditAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                Text(
                    text = "Bal: ${CurrencyFormatter.formatInr(item.runningBalance)}",
                    fontSize = 11.sp,
                    color = Slate600,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
