package com.example.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventoryCatalogItem
import com.example.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemsScreen(viewModel: LedgerViewModel, onBack: () -> Unit) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<InventoryCatalogItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var deleteItem by remember { mutableStateOf<InventoryCatalogItem?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Items, Price & Unit", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }, actions = { IconButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, "Add item") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, "Add item") } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Saved items are available automatically in the entry dropdown. You can add, edit or delete name, unit and default price here.", style = MaterialTheme.typography.bodyMedium) }
            if (items.isEmpty()) item { Text("No items yet. Add an item or save an inventory entry to build your item list.") }
            items(items, key = { it.id }) { item ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("Unit: ${item.unit}   •   Default Rate: ₹${item.defaultRate}")
                        }
                        IconButton(onClick = { editing = item; showDialog = true }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = { deleteItem = item }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
    if (showDialog) ItemEditorDialog(editing, onDismiss = { showDialog = false }, onSave = { name, unit, rate ->
        val old = editing
        if (old == null) viewModel.addInventoryItem(name, unit, rate)
        else viewModel.updateInventoryItem(old.copy(name = name.trim(), unit = unit.trim().ifBlank { "Pc" }, defaultRate = rate))
        showDialog = false
    })
    deleteItem?.let { item -> AlertDialog(onDismissRequest = { deleteItem = null }, title = { Text("Delete item?") }, text = { Text("${item.name} will be removed from the saved item dropdown. Old reports and entries will remain unchanged.") }, confirmButton = { TextButton(onClick = { viewModel.deleteInventoryItem(item); deleteItem = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteItem = null }) { Text("Cancel") } }) }
}

@Composable
private fun ItemEditorDialog(item: InventoryCatalogItem?, onDismiss: () -> Unit, onSave: (String, String, Double) -> Unit) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var unit by remember(item) { mutableStateOf(item?.unit ?: "Pc") }
    var rate by remember(item) { mutableStateOf(if (item == null || item.defaultRate == 0.0) "" else item.defaultRate.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (item == null) "Add New Item" else "Edit Item") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(unit, { unit = it }, label = { Text("Unit (Pc, Kg, etc.)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rate, { rate = it }, label = { Text("Default Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name, unit, rate.toDoubleOrNull() ?: 0.0) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
