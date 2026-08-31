package com.example.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(viewModel: LedgerViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var addingCustomCategory by remember { mutableStateOf(false) }

    val defaultCategories = listOf(
        "General Shop", "Grocery", "Clothing", "Wholesale", "Construction",
        "Machinery", "Transport", "Service / Repair", "Personal Ledger / Money Lending", "Other"
    )
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> logoUri = uri }
    val canCreate = name.isNotBlank() && category.isNotBlank() && mobile.isNotBlank() && email.isNotBlank()

    Scaffold(topBar = { TopAppBar(title = { Text("Set Up Your Shop") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Create your first shop to start My Ledger. Mobile number and email are saved in Settings. No demo data will be created.")

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(112.dp).clickable { picker.launch("image/*") },
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (logoUri != null) {
                        AsyncImage(model = logoUri, contentDescription = "Shop image", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text("Add\nPhoto") }
                    }
                }
            }
            TextButton(onClick = { picker.launch("image/*") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Choose Shop Owner Image / Logo")
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop / Business Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = category, onValueChange = {}, readOnly = true,
                    label = { Text("Business Category *") },
                    placeholder = { Text("Select or add your own category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    defaultCategories.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { category = c; customCategory = ""; addingCustomCategory = false; expanded = false })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("＋ Add your own category") }, onClick = { expanded = false; addingCustomCategory = true })
                }
            }

            if (addingCustomCategory) {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it; category = it },
                    label = { Text("Your Business Category") },
                    placeholder = { Text("e.g. Mobile Repair, Pharmacy") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            OutlinedTextField(
                value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Button(
                onClick = {
                    val customCats = if (addingCustomCategory && customCategory.isNotBlank()) customCategory.trim() else ""
                    viewModel.createBusiness(
                        name = name, address = address, city = "", state = "", pinCode = "",
                        mobile = mobile, email = email, gstNumber = "", upiId = "",
                        logoUrl = logoUri?.toString() ?: "",
                        businessCategory = category.trim(),
                        customCategoriesCsv = customCats
                    )
                    onComplete()
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Shop & Start Ledger") }
        }
    }
}
