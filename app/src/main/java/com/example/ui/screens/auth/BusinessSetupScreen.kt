package com.example.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(viewModel: LedgerViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("General Shop", "Grocery", "Clothing", "Wholesale", "Construction", "Machinery", "Transport", "Service / Repair", "Personal Ledger / Money Lending", "Other")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> logoUri = uri }

    Scaffold(topBar = { TopAppBar(title = { Text("Set Up Your Shop") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Create your first shop to start My Ledger. No demo company, customer or transaction will be added.")
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, modifier = Modifier.size(112.dp).clickable { picker.launch("image/*") }, color = MaterialTheme.colorScheme.primaryContainer) {
                    if (logoUri != null) AsyncImage(model = logoUri, contentDescription = "Shop logo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    else Box(contentAlignment = Alignment.Center) { Text("Add\nLogo") }
                }
            }
            TextButton(onClick = { picker.launch("image/*") }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Choose Shop Icon / Logo") }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop / Business Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Business Category *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; expanded = false }) }
                }
            }
            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Button(onClick = {
                viewModel.createBusiness(name, address, category, "", "", mobile, "", "", "", logoUri?.toString() ?: "")
                onComplete()
            }, enabled = name.isNotBlank() && category.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create Shop & Start Ledger") }
        }
    }
}
