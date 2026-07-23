package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import com.example.golfdistancetracker.ui.viewmodel.ClubStats
import com.example.golfdistancetracker.ui.viewmodel.StatsViewModel
import com.example.golfdistancetracker.util.UnitConverter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()
    val filters by viewModel.filters.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Performance Analytics") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filters
            FilterSection(filters.shotType) { viewModel.updateShotTypeFilter(it) }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(stats) { stat ->
                    if (stat.shots.isNotEmpty()) {
                        ClubStatsCard(stat)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(selectedType: ShotType?, onTypeSelect: (ShotType?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelect(null) },
                label = { Text("All") }
            )
        }
        item {
            FilterChip(
                selected = selectedType == ShotType.FIELD,
                onClick = { onTypeSelect(ShotType.FIELD) },
                label = { Text("Field") }
            )
        }
        item {
            FilterChip(
                selected = selectedType == ShotType.DRIVING_RANGE,
                onClick = { onTypeSelect(ShotType.DRIVING_RANGE) },
                label = { Text("Practice") }
            )
        }
    }
}

@Composable
fun ClubStatsCard(stat: ClubStats) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stat.club.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Avg Dist", UnitConverter.formatDistance(stat.averageDistance, stat.unit))
                MetricItem("Accuracy", String.format(Locale.US, "%.0f%%", stat.accuracyPct * 100))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Avg Dev", UnitConverter.formatDistance(stat.avgLatDev, stat.unit))
                MetricItem("Mishits", stat.mishitCount.toString())
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Shot Dispersion (Diana)", style = MaterialTheme.typography.labelMedium)
            ShotDispersionDiana(stat.shots)
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ShotDispersionDiana(shots: List<Shot>) {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            
            drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = radius * 0.66f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = radius * 0.33f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            
            drawLine(color = Color.LightGray, start = Offset(0f, center.y), end = Offset(size.width, center.y))
            drawLine(color = Color.LightGray, start = Offset(center.x, 0f), end = Offset(center.x, size.height))

            shots.forEach { shot ->
                // For Field shots, use lateralDeviation if available.
                // For Driving Range, use the -2 to 2 scale.
                val x = if (shot.shotType == ShotType.FIELD) {
                    val latDev = shot.lateralDeviation ?: 0.0
                    // Assume 20m is the edge of the chart
                    center.x + (latDev.toFloat() / 20f) * radius
                } else {
                    val dev = shot.deviation ?: 0f
                    center.x + (dev / 2f) * radius
                }
                
                val y = if (shot.shotType == ShotType.FIELD) {
                    center.y // Just center for field shots on X axis
                } else {
                    val qual = shot.quality ?: 1
                    center.y + (1 - qual) * (radius / 2f)
                }
                
                val color = when {
                    shot.isMishit -> Color.Red
                    shot.quality == 2 -> Color(0xFF2E7D32)
                    shot.quality == 1 -> Color(0xFF1976D2)
                    else -> Color.Gray
                }
                
                drawCircle(color = color, radius = 5f, center = Offset(x, y))
            }
        }
    }
}
