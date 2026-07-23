package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.ui.viewmodel.GolfBagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolfBagScreen(viewModel: GolfBagViewModel = hiltViewModel()) {
    val clubs by viewModel.clubs.collectAsState()
    var clubToEdit by remember { mutableStateOf<Club?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.bag_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (clubs.isEmpty()) {
            EmptyBagView()
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(clubs) { club ->
                    ClubItem(
                        club, 
                        onEdit = { clubToEdit = club },
                        onDelete = { viewModel.deleteClub(club) }
                    )
                }
            }
        }

        if (showAddDialog) {
            ClubDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, type, number, brand, model ->
                    viewModel.addClub(name, type, number, brand, model)
                    showAddDialog = false
                }
            )
        }

        if (clubToEdit != null) {
            ClubDialog(
                initialClub = clubToEdit,
                onDismiss = { clubToEdit = null },
                onSave = { name, type, number, brand, model ->
                    viewModel.updateClub(clubToEdit!!.copy(name = name, type = type, number = number, brand = brand, model = model))
                    clubToEdit = null
                }
            )
        }
    }
}

@Composable
fun EmptyBagView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.bag_empty), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.bag_empty_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ClubItem(club: Club, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when {
                club.type.contains("Driver", true) -> Icons.Default.SportsGolf
                club.type.contains("Putter", true) -> Icons.Default.VerticalAlignBottom
                club.type.contains("Wedge", true) -> Icons.Default.Terrain
                else -> Icons.Default.Hardware
            }
            
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(40.dp).padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${club.brand ?: stringResource(R.string.bag_generic)} • ${club.model ?: stringResource(R.string.bag_default)}", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.bag_edit), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.bag_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDialog(
    initialClub: Club? = null,
    onDismiss: () -> Unit, 
    onSave: (String, String, String?, String?, String?) -> Unit
) {
    var type by remember { mutableStateOf(initialClub?.type ?: "Iron") }
    var number by remember { mutableStateOf(initialClub?.number ?: "7") }
    var brand by remember { mutableStateOf(initialClub?.brand ?: "") }
    var model by remember { mutableStateOf(initialClub?.model ?: "") }

    val clubTypes = listOf("Driver", "Putter", "Hibrido", "Iron", "Wedge")
    val brands = listOf("Callaway", "TaylorMade", "Titleist", "Ping", "Mizuno", "Wilson", "Cobra", "Srixon", "Cleveland", "PXG", "Other")
    
    var typeExpanded by remember { mutableStateOf(false) }
    var numberExpanded by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }

    val numbers = when (type) {
        "Iron" -> (3..10).map { it.toString() }
        "Hibrido" -> (2..5).map { it.toString() }
        "Wedge" -> listOf("PW", "GW", "SW", "LW")
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialClub == null) stringResource(R.string.bag_add_club) else stringResource(R.string.bag_edit_club)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.bag_type)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        clubTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    type = selectionOption
                                    typeExpanded = false
                                    val newNumbers = when (type) {
                                        "Iron" -> (3..10).map { it.toString() }
                                        "Hibrido" -> (2..5).map { it.toString() }
                                        "Wedge" -> listOf("PW", "GW", "SW", "LW")
                                        else -> emptyList()
                                    }
                                    number = newNumbers.firstOrNull() ?: ""
                                }
                            )
                        }
                    }
                }

                // Number Dropdown (Conditional)
                if (numbers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = numberExpanded,
                        onExpandedChange = { numberExpanded = !numberExpanded }
                    ) {
                        OutlinedTextField(
                            value = number,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.bag_number)) },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = numberExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = numberExpanded, onDismissRequest = { numberExpanded = false }) {
                            numbers.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        number = selectionOption
                                        numberExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Brand Dropdown
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text(stringResource(R.string.bag_brand)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                        brands.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    brand = selectionOption
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = model, 
                    onValueChange = { model = it }, 
                    label = { Text(stringResource(R.string.bag_model)) }, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val name = if (type == "Putter" || type == "Driver") type else "$type $number"
                onSave(name, type, number.ifEmpty { null }, brand.ifEmpty { null }, model.ifEmpty { null }) 
            }) {
                Text(if (initialClub == null) stringResource(R.string.common_add) else stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
