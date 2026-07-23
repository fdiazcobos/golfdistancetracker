package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.ui.viewmodel.SessionViewModel
import com.example.golfdistancetracker.util.UnitConverter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(viewModel: SessionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val clubs by viewModel.clubs.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    Text(
                        uiState.selectedClub?.let { stringResource(R.string.session_tracking, it.name) } 
                        ?: stringResource(R.string.session_new)
                    ) 
                },
                actions = {
                    uiState.weather?.let { WeatherWidget(it) }
                    if (uiState.selectedClub != null) {
                        TextButton(onClick = { viewModel.resetSession() }) {
                            Text(stringResource(R.string.session_change_club))
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.selectedClub == null) {
                ClubSelectionGrid(clubs, uiState) { viewModel.selectClub(it) }
            } else {
                TrackingUI(
                    uiState = uiState,
                    onMarkStart = { viewModel.markStart() },
                    onMarkEnd = { viewModel.markEnd() },
                    onTargetChange = { viewModel.updateTargetDistance(it) }
                )
            }
        }
    }
}

@Composable
fun WeatherWidget(weather: com.example.golfdistancetracker.ui.viewmodel.WeatherInfo) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
        Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            "${weather.windSpeed.toInt()} km/h", 
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun ClubSelectionGrid(
    clubs: List<Club>, 
    uiState: com.example.golfdistancetracker.ui.viewmodel.SessionUiState,
    onSelect: (Club) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.session_select_club), style = MaterialTheme.typography.headlineSmall)
        
        if (uiState.recommendedClub != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.session_caddie_recommendation, uiState.recommendedClub.name),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clubs) { club ->
                val usage = uiState.clubUsage[club.id] ?: 0
                Button(
                    onClick = { onSelect(club) },
                    modifier = Modifier.height(72.dp),
                    colors = if (club == uiState.recommendedClub) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(club.name, maxLines = 1)
                        if (usage > 0) {
                            Text(
                                stringResource(R.string.session_used, usage), 
                                style = MaterialTheme.typography.labelSmall,
                                color = if (club == uiState.recommendedClub) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingUI(
    uiState: com.example.golfdistancetracker.ui.viewmodel.SessionUiState,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onTargetChange: (Double?) -> Unit
) {
    var targetInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val unitSuffix = if(uiState.distanceUnit == com.example.golfdistancetracker.data.prefs.DistanceUnit.YARDS) "yd" else "m"
            OutlinedTextField(
                value = targetInput,
                onValueChange = { 
                    targetInput = it
                    onTargetChange(it.toDoubleOrNull())
                },
                label = { Text(stringResource(R.string.session_target_dist, unitSuffix)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (uiState.playsLikeDistance != null && uiState.playsLikeDistance != uiState.targetDistanceMeters) {
                Text(
                    stringResource(R.string.session_plays_like, UnitConverter.formatDistance(uiState.playsLikeDistance, uiState.distanceUnit)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Visual Compass
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Color.Gray, radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                
                rotate(degrees = uiState.currentHeading) {
                    val path = Path().apply {
                        moveTo(centerOffset.x, 20f)
                        lineTo(centerOffset.x - 20f, 60f)
                        lineTo(centerOffset.x + 20f, 60f)
                        close()
                    }
                    drawPath(path, color = Color.Red)
                    drawLine(
                        color = Color.Red, 
                        start = centerOffset.copy(y = 60f), 
                        end = centerOffset.copy(y = size.height * 0.8f), 
                        strokeWidth = 8f
                    )
                }
            }
            Text("${uiState.currentHeading.toInt()}°", fontWeight = FontWeight.Bold)
        }

        // GPS Status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isGpsReady) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            )
        ) {
            Text(
                if (uiState.isGpsReady) stringResource(R.string.session_gps_ready) else stringResource(R.string.session_gps_waiting),
                modifier = Modifier.padding(8.dp),
                color = if (uiState.isGpsReady) Color(0xFF2E7D32) else Color(0xFFEF6C00)
            )
        }

        Text(stringResource(R.string.session_location, uiState.currentPosition?.let { String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) } ?: "..."))

        if (uiState.startLocation == null) {
            Button(
                onClick = onMarkStart,
                enabled = uiState.isGpsReady,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.session_mark_start))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.session_shot_progress), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onMarkEnd,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.session_mark_end))
                }
            }
        }

        uiState.lastShotDistance?.let {
            Text(
                stringResource(R.string.session_distance, UnitConverter.formatDistance(it, uiState.distanceUnit)),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
