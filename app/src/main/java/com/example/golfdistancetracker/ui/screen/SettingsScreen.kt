package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.data.prefs.DistanceUnit
import com.example.golfdistancetracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val currentUnit by viewModel.distanceUnit.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Distance Units", style = MaterialTheme.typography.titleLarge)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    UnitOption(
                        label = "Meters (m)",
                        selected = currentUnit == DistanceUnit.METERS,
                        onClick = { viewModel.updateUnit(DistanceUnit.METERS) }
                    )
                    UnitOption(
                        label = "Yards (yd)",
                        selected = currentUnit == DistanceUnit.YARDS,
                        onClick = { viewModel.updateUnit(DistanceUnit.YARDS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text("About", style = MaterialTheme.typography.titleLarge)
            Text("Golf Distance Tracker v1.0", style = MaterialTheme.typography.bodyMedium)
            Text("Built for Samsung S26 Ultra", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun UnitOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = onClick)
    }
}
