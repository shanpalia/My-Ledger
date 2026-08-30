package com.example.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
viewModel: LedgerViewModel,
onComplete: () -> Unit
) {
var name by remember { mutableStateOf("") }
var category by remember { mutableStateOf("") }
var mobile by remember { mutableStateOf("") }
var address by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

```
val categories = listOf(
    "General Shop",
    "Grocery",
    "Clothing",
    "Wholesale",
    "Construction",
    "Machinery",
    "Transport",
    "Service / Repair",
    "Personal Ledger / Money Lending",
    "Other"
)

Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text("Create Your Shop")
            }
        )
    }
) { paddingValues ->

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(20.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Set up your business first. No demo data will be created.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = {
                Text("Shop / Business Name *")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("Business Category *")
                },
                placeholder = {
                    Text("Select category")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                categories.forEach { selectedCategory ->
                    DropdownMenuItem(
                        text = {
                            Text(selectedCategory)
                        },
                        onClick = {
                            category = selectedCategory
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = {
                Text("Mobile Number *")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = {
                Text("Shop Address (Optional)")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = {
                viewModel.createBusiness(
                    name,
                    address,
                    "",
                    "",
                    "",
                    mobile,
                    "",
                    "",
                    ""
                )
                onComplete()
            },
            enabled =
                name.isNotBlank() &&
                category.isNotBlank() &&
                mobile.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Shop")
        }
    }
}
```

}
