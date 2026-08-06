package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.ui.viewmodel.GolfBagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolfBagScreen(viewModel: GolfBagViewModel = hiltViewModel()) {
    val clubs by viewModel.clubs.collectAsState()
    var clubToEdit by remember { mutableStateOf<Club?>(null) }
    var clubToDelete by remember { mutableStateOf<Club?>(null) }
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
                itemsIndexed(clubs) { index, club ->
                    ClubItem(
                        club = club, 
                        onEdit = { clubToEdit = club },
                        onDelete = { clubToDelete = club },
                        onMoveUp = { viewModel.moveClubUp(club) },
                        onMoveDown = { viewModel.moveClubDown(club) },
                        isFirst = index == 0,
                        isLast = index == clubs.size - 1
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

        if (clubToDelete != null) {
            AlertDialog(
                onDismissRequest = { clubToDelete = null },
                title = { Text("Delete Club?") },
                text = { Text("Are you sure you want to remove ${clubToDelete!!.name}? All recorded statistics for this club will be permanently deleted.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteClub(clubToDelete!!)
                            clubToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clubToDelete = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyBagView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1591491719183-8a994943bc7b?q=80&w=600&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.size(200.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.bag_empty), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.bag_empty_hint), 
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ClubItem(
    club: Club, 
    onEdit: () -> Unit, 
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.padding(12.dp), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder controls
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp, 
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, 
                        contentDescription = "Move Up",
                        tint = if (isFirst) Color.LightGray else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onMoveDown, 
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, 
                        contentDescription = "Move Down",
                        tint = if (isLast) Color.LightGray else MaterialTheme.colorScheme.primary
                    )
                }
            }

            val iconRes = when {
                club.type.contains("Driver", true) -> R.drawable.ic_club_driver
                club.type.contains("Putter", true) -> R.drawable.ic_club_putter
                club.type.contains("Wedge", true) -> R.drawable.ic_club_wedge
                club.type.contains("Hibrido", true) -> R.drawable.ic_club_hybrid
                else -> R.drawable.ic_club_iron
            }
            
            Icon(
                painter = painterResource(id = iconRes), 
                contentDescription = null, 
                modifier = Modifier.size(32.dp).padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${club.brand ?: stringResource(R.string.bag_generic)} • ${club.model ?: stringResource(R.string.bag_default)}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Row {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.bag_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
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
        "Driver" -> listOf("1", "2", "3")
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
                                        "Driver" -> listOf("1", "2", "3")
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
                        readOnly = true,
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
                val name = if (type == "Putter") type else "$type $number"
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
