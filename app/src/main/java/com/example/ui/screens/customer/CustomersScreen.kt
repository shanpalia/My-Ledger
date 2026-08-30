package com.example.ui.screens.customer

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BalanceType
import com.example.data.model.Customer
import com.example.data.model.TransactionType
import com.example.data.repository.CustomerBalanceSummary
import com.example.ui.theme.*
import com.example.ui.viewmodel.CustomerFilter
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.CurrencyFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: LedgerViewModel,
    onNavigateToCustomerLedger: (String) -> Unit,
    onNavigateToAddTransaction: (String, TransactionType) -> Unit,
    onOpenAddCustomerDialog: () -> Unit
) {
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.customerFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Indigo600,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                    Text("Add Customer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            // Search Bar & Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.customerSearchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_customer_input"),
                    placeholder = { Text("Search customer name, mobile or city...", fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate500)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.customerSearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Indigo600,
                        unfocusedBorderColor = BorderSlate200
                    )
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == CustomerFilter.ALL,
                        onClick = { viewModel.customerFilter.value = CustomerFilter.ALL },
                        label = { Text("All", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Indigo50,
                            selectedLabelColor = Indigo600
                        )
                    )
                    FilterChip(
                        selected = activeFilter == CustomerFilter.YOU_WILL_GET,
                        onClick = { viewModel.customerFilter.value = CustomerFilter.YOU_WILL_GET },
                        label = { Text("You'll Get", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DebitRed)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DebitRedContainer,
                            selectedLabelColor = DebitRed
                        )
                    )
                    FilterChip(
                        selected = activeFilter == CustomerFilter.YOU_WILL_GIVE,
                        onClick = { viewModel.customerFilter.value = CustomerFilter.YOU_WILL_GIVE },
                        label = { Text("You'll Give", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CreditGreen)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CreditGreenContainer,
                            selectedLabelColor = CreditGreen
                        )
                    )
                    FilterChip(
                        selected = activeFilter == CustomerFilter.SETTLED,
                        onClick = { viewModel.customerFilter.value = CustomerFilter.SETTLED },
                        label = { Text("Settled", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate100,
                            selectedLabelColor = Slate700
                        )
                    )
                }
            }

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PeopleOutline,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No customers match '$searchQuery'" else "No customers found",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap 'Add Customer' to start recording ledgers",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCustomers, key = { it.customer.id }) { summary ->
                        CustomerCardItem(
                            summary = summary,
                            onClick = { onNavigateToCustomerLedger(summary.customer.id) },
                            onAddDebit = { onNavigateToAddTransaction(summary.customer.id, TransactionType.DEBIT) },
                            onAddCredit = { onNavigateToAddTransaction(summary.customer.id, TransactionType.CREDIT) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, mobile, altMobile, address, openingBal, balType, notes ->
                viewModel.addCustomer(name, mobile, altMobile, address, openingBal, balType, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CustomerCardItem(
    summary: CustomerBalanceSummary,
    onClick: () -> Unit,
    onAddDebit: () -> Unit,
    onAddCredit: () -> Unit
) {
    val netBalance = summary.netBalance
    val isDebit = netBalance > 0.001
    val isCredit = netBalance < -0.001
    val isSettled = !isDebit && !isCredit

    val statusColor = when {
        isDebit -> DebitRed
        isCredit -> CreditGreen
        else -> Slate500
    }

    val statusText = when {
        isDebit -> "You'll Get"
        isCredit -> "You'll Give"
        else -> "Settled (0.00)"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${summary.customer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDebit -> DebitRedContainer
                                    isCredit -> CreditGreenContainer
                                    else -> Slate100
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.customer.name.take(2).uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = summary.customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summary.customer.mobile.ifEmpty { "No mobile" },
                            fontSize = 12.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                        if (summary.customer.address.isNotEmpty()) {
                            Text(
                                text = summary.customer.address,
                                fontSize = 11.sp,
                                color = Slate400Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Balance Info
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.formatInr(abs(netBalance)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = statusColor
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isDebit -> DebitRedContainer
                            isCredit -> CreditGreenContainer
                            else -> Slate100
                        }
                    ) {
                        Text(
                            text = statusText.uppercase(),
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSlate100, thickness = 1.dp)

            // Bottom action strip: Quick Given / Got buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddDebit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DebitRed),
                    border = BorderStroke(1.dp, DebitRed.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gave (Debit)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddCredit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CreditGreen),
                    border = BorderStroke(1.dp, CreditGreen.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.SouthWest, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Got (Credit)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "View Ledger", tint = Slate500)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, altMobile: String, address: String, openingBal: Double, balType: BalanceType, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var altMobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var openingBalStr by remember { mutableStateOf("") }
    var balType by remember { mutableStateOf(BalanceType.DEBIT) }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Add New Customer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Enter customer details and optional opening balance",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotEmpty()) nameError = false
                        },
                        label = { Text("Customer Name *", fontWeight = FontWeight.Medium) },
                        isError = nameError,
                        supportingText = { if (nameError) Text("Name is required") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_name"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number", fontWeight = FontWeight.Medium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_customer_mobile"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = altMobile,
                        onValueChange = { altMobile = it },
                        label = { Text("Alternate Mobile (Optional)", fontWeight = FontWeight.Medium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / City (Optional)", fontWeight = FontWeight.Medium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = openingBalStr,
                            onValueChange = { openingBalStr = it },
                            label = { Text("Opening Balance (₹)", fontWeight = FontWeight.Medium) },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = balType == BalanceType.DEBIT,
                                    onClick = { balType = BalanceType.DEBIT },
                                    label = { Text("Debit", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = balType == BalanceType.CREDIT,
                                    onClick = { balType = BalanceType.CREDIT },
                                    label = { Text("Credit", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Business Remarks", fontWeight = FontWeight.Medium) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Slate600)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.trim().isEmpty()) {
                                    nameError = true
                                    return@Button
                                }
                                val bal = openingBalStr.toDoubleOrNull() ?: 0.0
                                onConfirm(name, mobile, altMobile, address, bal, balType, notes)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            modifier = Modifier.testTag("button_save_customer")
                        ) {
                            Text("Save Customer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
