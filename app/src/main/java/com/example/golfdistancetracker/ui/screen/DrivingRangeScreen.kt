package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.golfdistancetracker.ui.viewmodel.DrivingRangeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingRangeScreen(viewModel: DrivingRangeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val clubs by viewModel.clubs.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.practice_title)) },
                actions = {
                    Column(
                        horizontalAlignment = Alignment.End, 
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.common_total_today), 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${uiState.dailyTotalShots}", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Club Selector
            Text(stringResource(R.string.practice_select_club), style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(clubs) { club ->
                    val usage = uiState.clubUsageToday[club.id] ?: 0
                    FilterChip(
                        selected = uiState.selectedClub == club,
                        onClick = { viewModel.selectClub(club) },
                        label = { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(club.name)
                                if (usage > 0) {
                                    Text("($usage)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                        modifier = Modifier.height(IntrinsicSize.Min)
                    )
                }
            }

            HorizontalDivider()

            // Direction
            Text(stringResource(R.string.practice_direction), style = MaterialTheme.typography.titleMedium)
            val deviationLabels = listOf(
                stringResource(R.string.practice_dev_far_left),
                stringResource(R.string.practice_dev_left),
                stringResource(R.string.practice_dev_center),
                stringResource(R.string.practice_dev_right),
                stringResource(R.string.practice_dev_far_right)
            )
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
            Text(stringResource(R.string.practice_quality), style = MaterialTheme.typography.titleMedium)
            val isPutter = uiState.selectedClub?.type == "Putter"
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!isPutter) {
                    FilterChip(
                        selected = uiState.isMishit,
                        onClick = { viewModel.toggleMishit(!uiState.isMishit) },
                        label = { Text(stringResource(R.string.practice_misshot)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
                    )
                    VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))
                }
                
                val qualityOptions = if (isPutter) {
                    listOf(
                        stringResource(R.string.practice_muy_corto) to -2, 
                        stringResource(R.string.practice_corto) to -1, 
                        stringResource(R.string.practice_bueno) to 0, 
                        stringResource(R.string.practice_largo) to 1, 
                        stringResource(R.string.practice_muy_largo) to 2
                    )
                } else {
                    listOf(
                        stringResource(R.string.practice_malo) to 0, 
                        stringResource(R.string.practice_bien) to 1, 
                        stringResource(R.string.practice_muy_bien) to 2
                    )
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
                Text(stringResource(R.string.practice_saved), color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                onClick = { viewModel.saveShot() },
                enabled = uiState.selectedClub != null,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.practice_register))
            }
        }
    }
}
