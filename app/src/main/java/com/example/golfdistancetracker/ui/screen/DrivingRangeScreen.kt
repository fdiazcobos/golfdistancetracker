package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.ui.viewmodel.DrivingRangeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingRangeScreen(viewModel: DrivingRangeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val clubs by viewModel.clubs.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Driving Range Practice") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Club Selector
            Text("Select Club", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clubs) { club ->
                    FilterChip(
                        selected = uiState.selectedClub == club,
                        onClick = { viewModel.selectClub(club) },
                        label = { Text(club.name) }
                    )
                }
            }

            Divider()

            // Direction
            Text("Direction / Deviation", style = MaterialTheme.typography.titleMedium)
            val deviationLabels = listOf("Mucho Izq", "Poco Izq", "Centro", "Poco Der", "Mucho Der")
            Column {
                Slider(
                    value = uiState.deviation,
                    onValueChange = { viewModel.updateDeviation(it) },
                    valueRange = -2f..2f,
                    steps = 3
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    deviationLabels.forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Quality
            Text("Shot Quality", style = MaterialTheme.typography.titleMedium)
            val isPutter = uiState.selectedClub?.type == "Putter"
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Mishit button FIRST as requested (Hide if Putter)
                if (!isPutter) {
                    FilterChip(
                        selected = uiState.isMishit,
                        onClick = { viewModel.toggleMishit(!uiState.isMishit) },
                        label = { Text("PIFIA") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
                    )
                    VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))
                }
                
                val qualityOptions = if (isPutter) {
                    listOf("Muy Corto" to -2, "Corto" to -1, "Bueno" to 0, "Largo" to 1, "Muy Largo" to 2)
                } else {
                    listOf("Malo" to 0, "Bien" to 1, "Muy Bien" to 2)
                }

                qualityOptions.forEach { (label, value) ->
                    Button(
                        onClick = { viewModel.updateQuality(value) },
                        colors = if (uiState.quality == value) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.saveSuccess) {
                Text("Shot Saved!", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                onClick = { viewModel.saveShot() },
                enabled = uiState.selectedClub != null,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Register Shot")
            }
        }
    }
}
