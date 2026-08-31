package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.LedgerTransaction
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LedgerViewModel,
    onNavigateToAddTransaction: (String?, TransactionType) -> Unit,
    onNavigateToAddCustomer: () -> Unit,
    onNavigateToCustomerLedger: (String) -> Unit,
    onNavigateToReports: () -> Unit,
    onOpenBusinessSwitcher: () -> Unit
) {
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val customerSummaries by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    // Calculate aggregated metrics
    val totalCustomers = customers.size
    val totalDebit = customerSummaries.values.sumOf { it.totalDebit }
    val totalCredit = customerSummaries.values.sumOf { it.totalCredit }
    val totalOutstanding = customerSummaries.values.sumOf { if (it.netBalance > 0) it.netBalance else 0.0 }
    val totalToGive = customerSummaries.values.sumOf { if (it.netBalance < 0) -it.netBalance else 0.0 }
    val netOverallBalance = totalOutstanding - totalToGive

    val startOfToday = DateUtils.getStartOfDay()
    val todayTransactions = transactions.filter { it.transactionDate >= startOfToday }
    val todayAmount = todayTransactions.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Business header. Owner-selected image appears only when the owner has chosen one.
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("business_header_card").clickable { onOpenBusinessSwitcher() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                val logo = activeBiz?.logoUrl.orEmpty()
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (logo.isNotBlank()) {
                        AsyncImage(
                            model = logo,
                            contentDescription = "Shop owner image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.align(Alignment.CenterStart).size(56.dp).clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = if (logo.isNotBlank()) 64.dp else 40.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = activeBiz?.businessName ?: "MY LEDGER",
                                color = Slate900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Switch Business", tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                        Text("MY LEDGER by shanpalia", color = Slate500, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Surface(
                        shape = CircleShape,
                        color = Slate100,
                        modifier = Modifier.align(Alignment.CenterEnd).size(40.dp).clickable { onOpenBusinessSwitcher() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Storefront, contentDescription = "Shops", tint = Slate600, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // 2. Bold Hero Banner: Indigo-700 rounded-[2rem] with Total Outstanding & To Receive / To Pay
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_total_outstanding"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo700),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Indigo700, Indigo800)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total Outstanding",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Indigo100.copy(alpha = 0.85f)
                        )

                        Text(
                            text = CurrencyFormatter.formatInr(totalOutstanding),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Divider & Split Sub-metrics
                        HorizontalDivider(
                            color = Indigo500.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: To Receive (Credit / Outstanding from customers)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "TO RECEIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Indigo200
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(totalOutstanding),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7) // emerald-300
                                )
                            }

                            // Vertical divider line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Indigo500.copy(alpha = 0.4f))
                            )

                            // Right: To Pay (Advances / Liabilities)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "TO PAY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Indigo200
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(totalToGive),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5) // red-300
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live customer balances: horizontally scrollable and updated from current transactions.
        if (customers.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Live Customer Balances", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                        Text("Tap a customer", color = Slate500, fontSize = 11.sp)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(customers, key = { it.id }) { customer ->
                            val balance = customerSummaries[customer.id]?.netBalance ?: 0.0
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier.clickable { onNavigateToCustomerLedger(customer.id) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, color = Slate900, maxLines = 1)
                                    Text(
                                        CurrencyFormatter.formatInr(kotlin.math.abs(balance)),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (balance > 0) DebitRed else if (balance < 0) CreditGreen else Slate500
                                    )
                                    Text(
                                        if (balance > 0) "Due" else if (balance < 0) "Advance" else "Settled",
                                        fontSize = 10.sp,
                                        color = Slate500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Action Cards: High-polish 2-Column Grid matching Design HTML
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Credit Action Card
                    ActionGridCard(
                        title = "Add Credit",
                        value = "You Got",
                        badgeBg = CreditGreenContainer,
                        iconColor = CreditGreen,
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                        testTag = "action_add_credit",
                        onClick = { onNavigateToAddTransaction(null, TransactionType.CREDIT) }
                    )

                    // Add Debit Action Card
                    ActionGridCard(
                        title = "Add Debit",
                        value = "You Gave",
                        badgeBg = DebitRedContainer,
                        iconColor = DebitRed,
                        icon = Icons.Default.Remove,
                        modifier = Modifier.weight(1f),
                        testTag = "action_add_debit",
                        onClick = { onNavigateToAddTransaction(null, TransactionType.DEBIT) }
                    )
                }

                // Secondary Row: Add Customer & View Reports
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionSecondaryCard(
                        title = "Add Customer",
                        subtitle = "$totalCustomers Registered",
                        icon = Icons.Default.PersonAdd,
                        accentColor = Indigo600,
                        modifier = Modifier.weight(1f),
                        testTag = "action_add_customer",
                        onClick = onNavigateToAddCustomer
                    )

                    ActionSecondaryCard(
                        title = "Reports & PDF",
                        subtitle = "Statements & Stats",
                        icon = Icons.Default.Assessment,
                        accentColor = Slate700,
                        modifier = Modifier.weight(1f),
                        testTag = "action_view_reports",
                        onClick = onNavigateToReports
                    )
                }
            }
        }

        // 4. Pending Payment Reminders Quick Bar
        val pendingCustomers = customerSummaries.values.filter { it.netBalance > 100 }.sortedByDescending { it.netBalance }
        if (pendingCustomers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(PendingAmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = PendingAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Pending Dues (${pendingCustomers.size})",
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatInr(pendingCustomers.sumOf { it.netBalance }),
                                color = DebitRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(pendingCustomers.take(6)) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BackgroundLight,
                                    modifier = Modifier.clickable { onNavigateToCustomerLedger(item.customer.id) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = item.customer.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatInr(item.netBalance),
                                            color = DebitRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Activity Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                TextButton(
                    onClick = onNavigateToReports,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "View All",
                        color = Indigo600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 6. Recent Activity List
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Indigo50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Indigo600,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "No entries recorded yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Tap Add Credit or Add Debit to record your first ledger transaction",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions.take(10)) { tx ->
                val customer = customers.find { it.id == tx.customerId }
                BoldTransactionRowItem(
                    transaction = tx,
                    customerName = customer?.name ?: "Customer",
                    customerMobile = customer?.mobile ?: "",
                    onClick = {
                        if (customer != null) {
                            onNavigateToCustomerLedger(customer.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActionGridCard(
    title: String,
    value: String,
    badgeBg: Color,
    iconColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Circular Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }
    }
}

@Composable
fun ActionSecondaryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BoldTransactionRowItem(
    transaction: LedgerTransaction,
    customerName: String,
    customerMobile: String,
    onClick: () -> Unit
) {
    val isDebit = transaction.transactionType == TransactionType.DEBIT.name
    val amountColor = if (isDebit) DebitRed else CreditGreen
    val typeLabel = if (isDebit) "Debit Entry" else "Credit Entry"
    val initials = customerName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "C" }
        .uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circular Initials Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDebit) Slate100 else Indigo50),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDebit) Slate700 else Indigo600
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$typeLabel • ${DateUtils.formatDateTime(transaction.transactionDate)}",
                        fontSize = 10.5.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${if (isDebit) "-" else "+"}${CurrencyFormatter.formatInr(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                Text(
                    text = transaction.paymentMode.ifEmpty { "Cash" },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400
                )
            }
        }
    }
}
