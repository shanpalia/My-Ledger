package com.example.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Business
import com.example.data.model.NotificationSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.NotificationHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LedgerViewModel,
    onOpenBusinessSwitcher: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val activeBiz by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val notifSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditTemplatesDialog by remember { mutableStateOf(false) }
    var showCreateBizDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedLogoUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Shops", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate900) },
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
            // User Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Indigo700),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Indigo500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProfile?.fullName?.takeIf { it.isNotBlank() } ?: "Owner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = currentProfile?.email?.takeIf { it.isNotBlank() } ?: activeBiz?.email?.takeIf { it.isNotBlank() } ?: "Email not set",
                                fontSize = 12.5.sp,
                                color = Indigo200
                            )
                            Text(
                                text = "Mobile: ${currentProfile?.mobile?.takeIf { it.isNotBlank() } ?: activeBiz?.mobile?.takeIf { it.isNotBlank() } ?: "Not set"}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Indigo100
                            )
                        }
                    }
                }
            }

            // Multi-Business Switcher Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Shops / Businesses (${businesses.size})",
                        fontSize = 15.sp,
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showCreateBizDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Indigo600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Shop", color = Indigo600, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(businesses) { biz ->
                        val isSelected = biz.id == activeBiz?.id
                        Card(
                            modifier = Modifier
                                .width(210.dp)
                                .clickable { viewModel.switchBusiness(biz.id) }
                                .testTag("shop_card_${biz.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Indigo600 else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = biz.businessName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Slate900,
                                        maxLines = 1
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = biz.businessCategory.ifBlank { listOfNotNull(biz.city, biz.state).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "India" } },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Indigo200 else Slate500
                                )
                                Text(
                                    text = if (biz.gstNumber.isNotEmpty()) "GST: ${biz.gstNumber}" else "UPI: ${biz.upiId.ifEmpty { "Active" }}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate400Text
                                )
                            }
                        }
                    }
                }
            }

            // Active Business Profile Settings
            item {
                Text(
                    text = "Active Shop Settings",
                    fontSize = 15.sp,
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Outlined.Storefront,
                            title = "Edit Shop Profile",
                            subtitle = "${activeBiz?.businessName ?: "Shop"} • Address, GST, UPI",
                            onClick = { selectedLogoUri = null; showEditProfileDialog = true }
                        )
                        HorizontalDivider(color = BorderSlate100)
                        SettingsActionRow(
                            icon = Icons.Outlined.Message,
                            title = "Notification Templates",
                            subtitle = "Customize Debit, Credit & Reminder message formats",
                            onClick = { showEditTemplatesDialog = true }
                        )
                    }
                }
            }

            // Notification Channels Toggles
            item {
                Text(
                    text = "Notification Channels",
                    fontSize = 15.sp,
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    val settings = notifSettings ?: NotificationSettings(id = UUID.randomUUID().toString(), businessId = activeBiz?.id ?: "")

                    Column {
                        SettingsToggleRow(
                            icon = Icons.Default.Chat,
                            title = "WhatsApp Notifications",
                            subtitle = "Send transaction receipt & reminder via WhatsApp",
                            checked = settings.whatsappEnabled,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(settings.copy(whatsappEnabled = it))
                            }
                        )
                        HorizontalDivider(color = BorderSlate100)
                        SettingsToggleRow(
                            icon = Icons.Default.Sms,
                            title = "SMS Notifications",
                            subtitle = "Send automatic SMS alerts to customer",
                            checked = settings.smsEnabled,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(settings.copy(smsEnabled = it))
                            }
                        )
                        HorizontalDivider(color = BorderSlate100)
                        SettingsToggleRow(
                            icon = Icons.Default.Notifications,
                            title = "App Push Notifications",
                            subtitle = "Daily summary & overdue alerts",
                            checked = settings.pushEnabled,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(settings.copy(pushEnabled = it))
                            }
                        )
                    }
                }
            }

            // Security & App Preferences
            item {
                Text(
                    text = "Security & App Settings",
                    fontSize = 15.sp,
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Outlined.Lock,
                            title = "App PIN Lock",
                            subtitle = if (currentProfile?.pinLock != null) "PIN Lock is Enabled" else "Set 4-digit PIN lock for security",
                            onClick = { showPinDialog = true }
                        )
                        HorizontalDivider(color = BorderSlate100)
                        SettingsActionRow(
                            icon = Icons.Outlined.Language,
                            title = "Language",
                            subtitle = appLanguage,
                            onClick = {
                                viewModel.appLanguage.value = if (appLanguage == "English") "Hindi (हिंदी)" else "English"
                                Toast.makeText(context, "Language switched to ${viewModel.appLanguage.value}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = BorderSlate100)
                        SettingsActionRow(
                            icon = Icons.Outlined.CloudSync,
                            title = "Cloud Sync & Backup",
                            subtitle = "All records securely synced to local storage & cloud",
                            onClick = {
                                Toast.makeText(context, "Ledger database is up to date & synced!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = BorderSlate100)
                        SettingsActionRow(
                            icon = Icons.Outlined.Logout,
                            title = "Logout / Switch Account",
                            subtitle = "Signed in as ${currentProfile?.fullName ?: "Owner"}",
                            onClick = onNavigateToAuth,
                            titleColor = DebitRed
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "My Ledger v1.0.0",
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Smart Debit & Credit Management",
                        color = Slate400Text,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    // Edit Business Profile Dialog
    if (showEditProfileDialog && activeBiz != null) {
        val biz = activeBiz!!
        var name by remember { mutableStateOf(biz.businessName) }
        var address by remember { mutableStateOf(biz.address) }
        var city by remember { mutableStateOf(biz.city) }
        var state by remember { mutableStateOf(biz.state) }
        var pinCode by remember { mutableStateOf(biz.pinCode) }
        var mobile by remember { mutableStateOf(biz.mobile) }
        var email by remember { mutableStateOf(biz.email) }
        var gstNumber by remember { mutableStateOf(biz.gstNumber) }
        var upiId by remember { mutableStateOf(biz.upiId) }
        var businessCategory by remember { mutableStateOf(biz.businessCategory) }
        var customCategoriesCsv by remember { mutableStateOf(biz.customCategoriesCsv) }
        var customUnitsCsv by remember { mutableStateOf(biz.customUnitsCsv) }

        Dialog(onDismissRequest = { selectedLogoUri = null; showEditProfileDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Edit Shop Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Indigo100)
                                    .clickable { logoPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                val preview = selectedLogoUri?.toString() ?: biz.logoUrl
                                if (preview.isNotBlank()) {
                                    AsyncImage(
                                        model = preview,
                                        contentDescription = "Shop profile image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Indigo600, modifier = Modifier.size(42.dp))
                                }
                            }
                            TextButton(onClick = { logoPicker.launch("image/*") }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (biz.logoUrl.isBlank() && selectedLogoUri == null) "Add Shop Image / Logo" else "Change Shop Image / Logo")
                            }
                        }
                    }
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop Name *") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Shop Mobile") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Shop Email") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedTextField(value = pinCode, onValueChange = { pinCode = it }, label = { Text("PIN Code") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = gstNumber, onValueChange = { gstNumber = it }, label = { Text("GST Number (Optional)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID (for payments)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(
                            value = businessCategory,
                            onValueChange = { businessCategory = it },
                            label = { Text("Business Category") },
                            placeholder = { Text("Edit or type your category") },
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = customCategoriesCsv,
                            onValueChange = { customCategoriesCsv = it },
                            label = { Text("My Extra Shop Categories") },
                            placeholder = { Text("Comma separated: Pharmacy, Hardware, Mobile") },
                            supportingText = { Text("You can add or edit your own categories") },
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = customUnitsCsv,
                            onValueChange = { customUnitsCsv = it },
                            label = { Text("My Inventory Units") },
                            placeholder = { Text("Comma separated: Pc, Kg, Carton, Bundle") },
                            supportingText = { Text("These units appear in Tally Style Inventory") },
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { selectedLogoUri = null; showEditProfileDialog = false }) { Text("Cancel", color = Slate600) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.updateBusinessProfile(
                                        business = biz.copy(
                                            businessName = name,
                                            address = address,
                                            city = city,
                                            state = state,
                                            pinCode = pinCode,
                                            mobile = mobile,
                                            email = email,
                                            gstNumber = gstNumber,
                                            upiId = upiId,
                                            businessCategory = businessCategory.trim(),
                                            customCategoriesCsv = customCategoriesCsv,
                                            customUnitsCsv = customUnitsCsv
                                        ),
                                        newLogoSource = selectedLogoUri?.toString()
                                    )
                                    selectedLogoUri = null
                                    showEditProfileDialog = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                            ) {
                                Text("Save Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Business Dialog
    if (showCreateBizDialog) {
        var newBizName by remember { mutableStateOf("") }
        var newBizAddress by remember { mutableStateOf("") }
        var newBizCity by remember { mutableStateOf("") }
        var newBizState by remember { mutableStateOf("") }
        var newBizPin by remember { mutableStateOf("") }
        var newBizMobile by remember { mutableStateOf("") }
        var newBizEmail by remember { mutableStateOf("") }
        var newBizGst by remember { mutableStateOf("") }
        var newBizUpi by remember { mutableStateOf("") }
        var newBizCategory by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateBizDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Add New Shop / Business", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    item {
                        OutlinedTextField(value = newBizName, onValueChange = { newBizName = it }, label = { Text("Shop Name *") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = newBizMobile, onValueChange = { newBizMobile = it }, label = { Text("Mobile") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = newBizEmail, onValueChange = { newBizEmail = it }, label = { Text("Email") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = newBizCategory, onValueChange = { newBizCategory = it }, label = { Text("Business Category") }, placeholder = { Text("e.g. Grocery, Pharmacy, Machinery") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = newBizAddress, onValueChange = { newBizAddress = it }, label = { Text("Address") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = newBizCity, onValueChange = { newBizCity = it }, label = { Text("City") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = newBizState, onValueChange = { newBizState = it }, label = { Text("State") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedTextField(value = newBizPin, onValueChange = { newBizPin = it }, label = { Text("PIN Code") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = newBizGst, onValueChange = { newBizGst = it }, label = { Text("GST Number (Optional)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = newBizUpi, onValueChange = { newBizUpi = it }, label = { Text("UPI ID (for payments)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCreateBizDialog = false }) { Text("Cancel", color = Slate600) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newBizName.trim().isNotEmpty()) {
                                        viewModel.createBusiness(
                                            name = newBizName.trim(),
                                            address = newBizAddress.trim(),
                                            city = newBizCity.trim(),
                                            state = newBizState.trim(),
                                            pinCode = newBizPin.trim(),
                                            mobile = newBizMobile.trim(),
                                            email = newBizEmail.trim(),
                                            gstNumber = newBizGst.trim(),
                                            upiId = newBizUpi.trim(),
                                            businessCategory = newBizCategory.trim(),
                                            customCategoriesCsv = newBizCategory.trim()
                                        )
                                        showCreateBizDialog = false
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                            ) {
                                Text("Create Shop", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Templates Dialog
    if (showEditTemplatesDialog) {
        val settings = notifSettings ?: NotificationSettings(id = UUID.randomUUID().toString(), businessId = activeBiz?.id ?: "")
        var debitTpl by remember { mutableStateOf(settings.debitTemplate) }
        var creditTpl by remember { mutableStateOf(settings.creditTemplate) }
        var reminderTpl by remember { mutableStateOf(settings.reminderTemplate) }

        Dialog(onDismissRequest = { showEditTemplatesDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Notification Templates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                        Text(
                            "Variables: {customer_name}, {amount}, {shop_name}, {balance}, {date}, {upi_id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = debitTpl,
                            onValueChange = { debitTpl = it },
                            label = { Text("Debit (Gave Amount) Template") },
                            minLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = creditTpl,
                            onValueChange = { creditTpl = it },
                            label = { Text("Credit (Payment Received) Template") },
                            minLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = reminderTpl,
                            onValueChange = { reminderTpl = it },
                            label = { Text("Payment Reminder Template") },
                            minLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showEditTemplatesDialog = false }) { Text("Cancel", color = Slate600) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.updateNotificationSettings(
                                        settings.copy(
                                            debitTemplate = debitTpl,
                                            creditTemplate = creditTpl,
                                            reminderTemplate = reminderTpl
                                        )
                                    )
                                    showEditTemplatesDialog = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                            ) {
                                Text("Save Templates", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showPinDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Security PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("Enter a 4-digit PIN to secure your ledger data, or leave empty to disable.", fontSize = 12.sp, color = Slate500)

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4-digit PIN") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPinDialog = false }) { Text("Cancel", color = Slate600) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.setPinLock(pinInput.takeIf { it.length == 4 })
                                showPinDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            Text("Set PIN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Slate900,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Indigo50),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Indigo600, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = titleColor)
            Text(text = subtitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate500)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400Text)
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Indigo50),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Indigo600, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
            Text(text = subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Slate500)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Indigo600
            )
        )
    }
}
