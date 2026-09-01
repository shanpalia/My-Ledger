package com.example.ui.screens.transaction

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationChannel
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlin.math.abs

private data class InventoryLine(val itemName: String, val quantity: Double, val unit: String, val rate: Double) {
    val amount: Double get() = quantity * rate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    initialCustomerId: String?,
    initialType: TransactionType = TransactionType.DEBIT,
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLedger: (String) -> Unit,
    onAddNewCustomer: () -> Unit
) {
    val context = LocalContext.current
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val summaries by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val notifSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()
    val savedItems by viewModel.inventoryItems.collectAsStateWithLifecycle()

    var selectedCustomerId by remember { mutableStateOf(initialCustomerId ?: customers.firstOrNull()?.id ?: "") }
    var transactionType by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var referenceNumber by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var inventoryMode by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var itemUnit by remember { mutableStateOf("Pc") }
    var customUnit by remember { mutableStateOf("") }
    var addingCustomUnit by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }
    var inventoryLines by remember { mutableStateOf(listOf<InventoryLine>()) }

    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var sendNotification by remember { mutableStateOf(false) }
    var notificationChannel by remember { mutableStateOf("WhatsApp") }
    var notificationChannelExpanded by remember { mutableStateOf(false) }

    val baseUnits = listOf("Pc", "Kg", "Gram", "Litre", "Ml", "Box", "Pack", "Dozen", "Meter", "Feet", "Bag")
    val savedCustomUnits = activeBiz?.customUnitsCsv.orEmpty()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val availableUnits = (baseUnits + savedCustomUnits).distinctBy { it.lowercase() }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val currentCustomerSummary = summaries[selectedCustomerId]
    val previousBalance = currentCustomerSummary?.netBalance ?: 0.0

    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val inventoryTotal = inventoryLines.sumOf { it.amount }
    val inventoryMatchesAmount = !inventoryMode || (inventoryLines.isNotEmpty() && kotlin.math.abs(enteredAmount - inventoryTotal) < 0.01)
    val newBalance = if (transactionType == TransactionType.DEBIT) {
        previousBalance + enteredAmount
    } else {
        previousBalance - enteredAmount
    }

    val isDebit = transactionType == TransactionType.DEBIT
    val themeColor = if (isDebit) DebitRed else CreditGreen
    val themeContainer = if (isDebit) DebitRedContainer else CreditGreenContainer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isDebit) "Add Debit (You Gave)" else "Add Credit (You Got)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate900
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            if (selectedCustomerId.isEmpty()) {
                                Toast.makeText(context, "Please select a customer", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val amt = amountStr.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                amountError = true
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (inventoryMode && inventoryLines.isEmpty()) {
                                Toast.makeText(context, "Add at least one inventory item", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (inventoryMode && !inventoryMatchesAmount) {
                                Toast.makeText(context, "Tally mismatch: entry amount and inventory total must be exactly the same", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            isSaving = true
                            viewModel.addTransaction(
                                customerId = selectedCustomerId,
                                type = transactionType,
                                amount = amt,
                                description = buildString {
                                    if (inventoryLines.isNotEmpty()) {
                                        append(inventoryLines.joinToString("\n") { "${it.itemName} | ${it.quantity} ${it.unit} × ${it.rate} = ${it.amount}" })
                                        if (description.isNotBlank()) append("\n")
                                    }
                                    append(description)
                                },
                                paymentMode = paymentMode,
                                referenceNumber = referenceNumber,
                                transactionDate = transactionDate,
                                createNotification = sendNotification,
                                onSuccess = { notifRecord ->
                                    isSaving = false
                                    // If notification enabled, offer quick send
                                    if (sendNotification && notifRecord != null && selectedCustomer?.mobile?.isNotEmpty() == true) {
                                        when (notificationChannel) {
                                            "SMS" -> NotificationHelper.openSms(context, selectedCustomer.mobile, notifRecord.message)
                                            else -> NotificationHelper.openWhatsApp(context, selectedCustomer.mobile, notifRecord.message)
                                        }
                                    }
                                    onNavigateToLedger(selectedCustomerId)
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("button_save_transaction"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        enabled = !isSaving && inventoryMatchesAmount
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sendNotification) "Save & Notify Customer" else "Save Entry",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional customer notification. Inventory lines are included in the generated message.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Notify customer after saving", fontWeight = FontWeight.Bold, color = Slate900)
                                Text("Optional. Inventory items and amount will be included in the message.", fontSize = 11.sp, color = Slate500)
                            }
                            Switch(checked = sendNotification, onCheckedChange = { sendNotification = it })
                        }
                        if (sendNotification) {
                            ExposedDropdownMenuBox(
                                expanded = notificationChannelExpanded,
                                onExpandedChange = { notificationChannelExpanded = !notificationChannelExpanded }
                            ) {
                                OutlinedTextField(
                                    value = notificationChannel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Send via") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(notificationChannelExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(expanded = notificationChannelExpanded, onDismissRequest = { notificationChannelExpanded = false }) {
                                    listOf("WhatsApp", "SMS").forEach { channel ->
                                        DropdownMenuItem(text = { Text(channel) }, onClick = { notificationChannel = channel; notificationChannelExpanded = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Type Selector Switcher
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { transactionType = TransactionType.DEBIT },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDebit) DebitRed else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ArrowOutward,
                                    contentDescription = null,
                                    tint = if (isDebit) Color.White else Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEBIT (You Gave)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (isDebit) Color.White else Slate600
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { transactionType = TransactionType.CREDIT },
                            shape = RoundedCornerShape(14.dp),
                            color = if (!isDebit) CreditGreen else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.SouthWest,
                                    contentDescription = null,
                                    tint = if (!isDebit) Color.White else Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CREDIT (You Got)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (!isDebit) Color.White else Slate600
                                )
                            }
                        }
                    }
                }
            }

            // Customer Selector Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = isCustomerDropdownExpanded,
                    onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer", fontWeight = FontWeight.Bold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("select_customer_dropdown"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Indigo600,
                            unfocusedBorderColor = BorderSlate200
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isCustomerDropdownExpanded,
                        onDismissRequest = { isCustomerDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("＋ Add New Customer", fontWeight = FontWeight.Bold, color = Indigo600) },
                            onClick = { isCustomerDropdownExpanded = false; onAddNewCustomer() }
                        )
                        HorizontalDivider()
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold)
                                            Text(cust.mobile, fontSize = 11.sp, color = Slate500)
                                        }
                                        val custBal = summaries[cust.id]?.netBalance ?: 0.0
                                        Text(
                                            CurrencyFormatter.formatInr(abs(custBal)),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (custBal > 0) DebitRed else if (custBal < 0) CreditGreen else Slate500
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCustomerId = cust.id
                                    isCustomerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isDebit) "ENTER AMOUNT GIVEN (₹)" else "ENTER AMOUNT RECEIVED (₹)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = Slate500
                        )

                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = {
                                amountStr = it
                                if (it.isNotEmpty()) amountError = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_transaction_amount"),
                            placeholder = { Text("0.00", fontSize = 28.sp, color = Slate400Text) },
                            prefix = {
                                Text(
                                    "₹ ",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = amountError,
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundLight,
                                unfocusedContainerColor = BackgroundLight
                            )
                        )

                        // Quick Amount Increment Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            val chips = listOf(100, 500, 1000, 2000, 5000, 10000)
                            items(chips) { chipVal ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate100,
                                    modifier = Modifier.clickable {
                                        val current = amountStr.toDoubleOrNull() ?: 0.0
                                        amountStr = (current + chipVal).toInt().toString()
                                        amountError = false
                                    }
                                ) {
                                    Text(
                                        text = "+₹$chipVal",
                                        color = Slate800,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment Mode Selector
            item {
                Text(
                    text = "Payment Mode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode.displayName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo50,
                                selectedLabelColor = Indigo600
                            )
                        )
                    }
                }
            }

            // Tally Style Inventory / Manual Item Entry
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = inventoryMode, onCheckedChange = { inventoryMode = it })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Tally Style Inventory", fontWeight = FontWeight.Bold)
                                Text("Item total must exactly match the amount above before saving.", fontSize = 11.sp, color = Slate500)
                            }
                        }

                        if (inventoryMode) {
                            Text("Item entry", fontWeight = FontWeight.Bold, color = Slate900)
                            ExposedDropdownMenuBox(
                                expanded = itemExpanded,
                                onExpandedChange = { itemExpanded = !itemExpanded }
                            ) {
                                OutlinedTextField(
                                    value = itemName,
                                    onValueChange = { itemName = it; itemExpanded = true },
                                    label = { Text("Item Name") },
                                    placeholder = { Text("Type new item or select saved item") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(itemExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = itemExpanded, onDismissRequest = { itemExpanded = false }) {
                                    val matches = savedItems.filter { it.name.contains(itemName, ignoreCase = true) }
                                    if (matches.isEmpty()) {
                                        DropdownMenuItem(text = { Text("No saved item — type a new item") }, onClick = { itemExpanded = false })
                                    } else matches.forEach { saved ->
                                        DropdownMenuItem(
                                            text = { Column { Text(saved.name); Text("${saved.unit} • ₹${saved.defaultRate}", style = MaterialTheme.typography.labelSmall) } },
                                            onClick = {
                                                itemName = saved.name
                                                itemUnit = saved.unit
                                                itemPrice = saved.defaultRate.toString()
                                                itemExpanded = false
                                            }
                                        )
                                    }
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = itemQty,
                                    onValueChange = { itemQty = it },
                                    label = { Text("Quantity") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = unitExpanded,
                                    onExpandedChange = { unitExpanded = !unitExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = itemUnit,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Unit") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                        availableUnits.forEach { unit ->
                                            DropdownMenuItem(text = { Text(unit) }, onClick = {
                                                itemUnit = unit
                                                addingCustomUnit = false
                                                unitExpanded = false
                                            })
                                        }
                                        HorizontalDivider()
                                        DropdownMenuItem(text = { Text("＋ Add custom unit") }, onClick = {
                                            unitExpanded = false
                                            addingCustomUnit = true
                                        })
                                    }
                                }
                                if (addingCustomUnit) {
                                    OutlinedTextField(
                                        value = customUnit,
                                        onValueChange = {
                                            customUnit = it
                                            if (it.isNotBlank()) itemUnit = it
                                        },
                                        label = { Text("Custom Unit") },
                                        placeholder = { Text("e.g. Bundle, Carton") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = itemPrice,
                                    onValueChange = { itemPrice = it },
                                    label = { Text("Rate") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val lineTotal = (itemQty.toDoubleOrNull() ?: 0.0) * (itemPrice.toDoubleOrNull() ?: 0.0)
                            Text("Line Amount: ${CurrencyFormatter.formatInr(lineTotal)}", fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = {
                                    val q = itemQty.toDoubleOrNull() ?: 0.0
                                    val p = itemPrice.toDoubleOrNull() ?: 0.0
                                    if (itemName.isNotBlank() && q > 0 && p >= 0) {
                                        inventoryLines = inventoryLines + InventoryLine(itemName.trim(), q, itemUnit, p)
                                        // Make a newly typed item available in future entry dropdowns too.
                                        viewModel.addInventoryItem(itemName.trim(), itemUnit, p)
                                        itemName = ""
                                        itemQty = ""
                                        itemPrice = ""
                                        itemUnit = "Pc"
                                    } else {
                                        Toast.makeText(context, "Enter item name, quantity and rate", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add Item") }

                            if (inventoryLines.isNotEmpty()) {
                                HorizontalDivider(color = BorderSlate100)
                                Text("Inventory", fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Item", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    Text("Qty", modifier = Modifier.weight(.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    Text("Unit", modifier = Modifier.weight(.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    Text("Rate", modifier = Modifier.weight(.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    Text("Total", modifier = Modifier.weight(.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                }
                                inventoryLines.forEachIndexed { index, line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(line.itemName, modifier = Modifier.weight(1.5f), fontSize = 11.sp, maxLines = 2)
                                        Text(line.quantity.toString(), modifier = Modifier.weight(.55f), fontSize = 11.sp)
                                        Text(line.unit, modifier = Modifier.weight(.55f), fontSize = 11.sp)
                                        Text(CurrencyFormatter.formatInr(line.rate), modifier = Modifier.weight(.8f), fontSize = 11.sp)
                                        Column(modifier = Modifier.weight(.85f), horizontalAlignment = Alignment.End) {
                                            Text(CurrencyFormatter.formatInr(line.amount), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            TextButton(onClick = { inventoryLines = inventoryLines.toMutableList().also { it.removeAt(index) } }, contentPadding = PaddingValues(0.dp)) { Text("Remove", fontSize = 9.sp) }
                                        }
                                    }
                                    HorizontalDivider(color = BorderSlate100)
                                }
                                Text("Inventory Total: ${CurrencyFormatter.formatInr(inventoryTotal)}", fontWeight = FontWeight.ExtraBold, color = Indigo600)
                                val amountText = if (amountStr.isBlank()) "₹0" else CurrencyFormatter.formatInr(enteredAmount)
                                val matchText = if (inventoryMatchesAmount) "MATCHED ✓" else "MISMATCH ✕"
                                val matchColor = if (inventoryMatchesAmount) CreditGreen else DebitRed
                                Text(
                                    "Entry Amount: $amountText  |  $matchText",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = matchColor
                                )
                                if (!inventoryMatchesAmount) {
                                    Text("Save is locked until Entry Amount = Inventory Total.", fontSize = 12.sp, color = DebitRed)
                                }
                            }
                        }
                    }
                }
            }

            // Description & Reference Number
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Item Details (Optional)", fontWeight = FontWeight.Medium) },
                        placeholder = { Text("e.g. 50kg Sugar, Bill #402, Cash paid") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_transaction_description"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Indigo600,
                            unfocusedBorderColor = BorderSlate200
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Bill No / UPI Ref ID (Optional)", fontWeight = FontWeight.Medium) },
                        placeholder = { Text("e.g. UPI883719 / INV-990") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Indigo600,
                            unfocusedBorderColor = BorderSlate200
                        ),
                        singleLine = true
                    )
                }
            }

            // Transaction Preview Card (Live calculation before saving)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = themeContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Transaction Preview & Effect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = themeColor
                            )
                        }

                        HorizontalDivider(color = themeColor.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Customer:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                            Text(selectedCustomer?.name ?: "-", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transaction Type:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                            Text(
                                if (isDebit) "DEBIT (+ Given)" else "CREDIT (- Got)",
                                fontWeight = FontWeight.Bold,
                                color = themeColor,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Previous Balance:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                            Text(CurrencyFormatter.formatInr(previousBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("New Balance:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(
                                CurrencyFormatter.formatInr(newBalance),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (newBalance > 0) DebitRed else if (newBalance < 0) CreditGreen else Slate900
                            )
                        }

                        Text(
                            text = if (isDebit)
                                "💡 This debit adds to what ${selectedCustomer?.name ?: "the customer"} owes your shop."
                            else
                                "💡 This credit reduces ${selectedCustomer?.name ?: "the customer"}'s pending debt or counts as advance.",
                            fontSize = 11.sp,
                            color = Slate600,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
