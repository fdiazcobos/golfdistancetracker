package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import com.example.golfdistancetracker.ui.component.ShotHeatmap
import com.example.golfdistancetracker.ui.viewmodel.*
import com.example.golfdistancetracker.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val clubStats by viewModel.clubStats.collectAsState()
    val courseStats by viewModel.courseStats.collectAsState()
    val historySessions by viewModel.historySessions.collectAsState()
    val filters by viewModel.filters.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var statsMode by remember { mutableIntStateOf(0) } // 0: Clubs, 1: Courses, 2: History

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = statsMode) {
                Tab(selected = statsMode == 0, onClick = { statsMode = 0 }, text = { Text("Clubs") })
                Tab(selected = statsMode == 1, onClick = { statsMode = 1 }, text = { Text("Courses") })
                Tab(selected = statsMode == 2, onClick = { statsMode = 2 }, text = { Text("History") })
            }

            when (statsMode) {
                0 -> {
                    Column {
                        FilterSection(filters.shotType) { viewModel.updateShotTypeFilter(it) }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                if (filters.shotType == ShotType.DRIVING_RANGE) {
                                    PracticeLoadSection(clubStats)
                                } else {
                                    GappingAnalysisSection(clubStats)
                                }
                            }
                            items(clubStats) { stat ->
                                if (stat.shots.isNotEmpty()) {
                                    ClubStatsCard(
                                        stat = stat,
                                        includeMisshots = filters.includeMisshotsInDispersion,
                                        showHeatmap = filters.showHeatmap,
                                        onToggleMisshots = { viewModel.toggleMisshotsInDispersion(it) },
                                        onToggleHeatmap = { viewModel.toggleHeatmap(it) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(courseStats) { analytics ->
                            CourseAnalyticsCard(analytics)
                        }
                    }
                }
                2 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(historySessions) { session ->
                            HistorySessionCard(session)
                        }
                    }
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.stats_reset)) },
                text = { Text(stringResource(R.string.stats_reset_confirm)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllStats()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.bag_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }
    }
}

@Composable
fun FilterSection(selectedType: ShotType?, onTypeSelect: (ShotType?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelect(null) },
            label = { Text(stringResource(R.string.stats_filter_all)) }
        )
        FilterChip(
            selected = selectedType == ShotType.FIELD,
            onClick = { onTypeSelect(ShotType.FIELD) },
            label = { Text(stringResource(R.string.stats_filter_field)) }
        )
        FilterChip(
            selected = selectedType == ShotType.DRIVING_RANGE,
            onClick = { onTypeSelect(ShotType.DRIVING_RANGE) },
            label = { Text(stringResource(R.string.stats_filter_practice)) }
        )
    }
}

@Composable
fun HistorySessionCard(session: HistorySession) {
    val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(sdf.format(Date(session.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                Surface(
                    color = if (session.type == SessionType.PLAY) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(session.type.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${session.shotsCount} Shots", style = MaterialTheme.typography.bodyMedium)
                    if (session.type == SessionType.PRACTICE) {
                        Text("Accuracy: ${(session.accuracy * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                session.trend?.let { trend ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPositive = trend > 0.02 // 2% buffer
                        Icon(
                            if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) Color(0xFF2E7D32) else Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isPositive) "Improving" else "Below Avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPositive) Color(0xFF2E7D32) else Color.Red
                        )
                    }
                }
            }
            
            if (session.type == SessionType.PRACTICE) {
                Spacer(Modifier.height(12.dp))
                QualityBar(session.qualityBreakdown)
            }
        }
    }
}

@Composable
fun CourseAnalyticsCard(analytics: CourseAnalytics) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(analytics.course.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(analytics.course.location ?: "Unknown Location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Avg Score", String.format(Locale.US, "%.1f", analytics.averageScore ?: 0.0))
                MetricItem("Best Round", analytics.bestScore?.toString() ?: "-")
                MetricItem("Rounds", analytics.roundsCount.toString())
            }
        }
    }
}

@Composable
fun ClubStatsCard(
    stat: ClubStats, 
    includeMisshots: Boolean, 
    showHeatmap: Boolean,
    onToggleMisshots: (Boolean) -> Unit,
    onToggleHeatmap: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stat.club.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(R.string.stats_avg_dist), UnitConverter.formatDistance(stat.averageDistance, stat.unit))
                MetricItem(stringResource(R.string.stats_accuracy), String.format(Locale.US, "%.0f%%", stat.accuracyPct * 100))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(R.string.stats_avg_dev), UnitConverter.formatDistance(stat.avgLatDev, stat.unit))
                MetricItem(stringResource(R.string.stats_mishits), stat.mishitCount.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.stats_quality_breakdown), style = MaterialTheme.typography.labelMedium)
            QualityBar(stat.qualityBreakdown)
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.stats_dispersion), style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleHeatmap(!showHeatmap) }) {
                        Icon(
                            if (showHeatmap) Icons.Default.GpsFixed else Icons.Default.GridOn, 
                            contentDescription = "Toggle Heatmap",
                            tint = if (showHeatmap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text("Misses", style = MaterialTheme.typography.labelSmall)
                    Checkbox(checked = includeMisshots, onCheckedChange = onToggleMisshots)
                }
            }
            
            val displayedShots = if (includeMisshots) stat.shots else stat.shots.filter { !it.isMishit }
            if (showHeatmap) {
                ShotHeatmap(displayedShots)
            } else {
                ShotDispersionDiana(displayedShots)
            }
        }
    }
}

@Composable
fun PracticeLoadSection(stats: List<ClubStats>) {
    val practiceStats = stats.filter { it.shots.isNotEmpty() }
    if (practiceStats.isEmpty()) return

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Practice Volume & Consistency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        
        practiceStats.forEach { stat ->
            val totalBalls = stat.shots.size
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stat.club.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$totalBalls balls", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), 
                                RoundedCornerShape(8.dp)
                            )
                    )
                    
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center) {
                        QualityBar(stat.qualityBreakdown)
                    }
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun GappingAnalysisSection(stats: List<ClubStats>) {
    val filteredStats = stats.filter { it.averageDistance != null }
    if (filteredStats.size < 2) return

    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.stats_gapping), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        filteredStats.forEach { stat ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(stat.club.name, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium)
                
                Box(modifier = Modifier.weight(1f).height(24.dp)) {
                    val maxDist = filteredStats.maxOf { it.averageDistance ?: 1.0 }
                    val progress = (stat.averageDistance ?: 0.0) / maxDist
                    
                    Box(modifier = Modifier.fillMaxWidth(progress.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    
                    Text(
                        UnitConverter.formatDistance(stat.averageDistance, stat.unit), 
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        color = if (progress > 0.8) Color.White else Color.Black,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            stat.gapToNext?.let { gap ->
                val isLargeGap = gap > 15.0
                
                Row(modifier = Modifier.padding(start = 80.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isLargeGap) Icons.Default.Warning else Icons.Default.VerticalAlignBottom,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isLargeGap) Color(0xFFD32F2F) else Color.Gray
                    )
                    Text(
                        stringResource(R.string.stats_gap, UnitConverter.formatDistance(gap, stat.unit)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLargeGap) Color(0xFFD32F2F) else Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    if (isLargeGap) Text(stringResource(R.string.stats_large_gap), color = Color(0xFFD32F2F), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun QualityBar(breakdown: QualityBreakdown) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp))) {
            if (breakdown.misshotPct > 0) Box(modifier = Modifier.weight(breakdown.misshotPct.toFloat()).fillMaxHeight().background(Color.Red))
            if (breakdown.poorPct > 0) Box(modifier = Modifier.weight(breakdown.poorPct.toFloat()).fillMaxHeight().background(Color.Gray))
            if (breakdown.goodPct > 0) Box(modifier = Modifier.weight(breakdown.goodPct.toFloat()).fillMaxHeight().background(Color(0xFF1976D2)))
            if (breakdown.greatPct > 0) Box(modifier = Modifier.weight(breakdown.greatPct.toFloat()).fillMaxHeight().background(Color(0xFF2E7D32)))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QualityLegendItem(stringResource(R.string.practice_misshot), breakdown.misshotPct, Color.Red)
            QualityLegendItem(stringResource(R.string.practice_malo), breakdown.poorPct, Color.Gray)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QualityLegendItem(stringResource(R.string.practice_bien), breakdown.goodPct, Color(0xFF1976D2))
            QualityLegendItem(stringResource(R.string.practice_muy_bien), breakdown.greatPct, Color(0xFF2E7D32))
        }
    }
}

@Composable
fun QualityLegendItem(label: String, percent: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(
            "$label: ${(percent * 100).toInt()}%", 
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                val x = if (shot.shotType == ShotType.FIELD) {
                    val latDev = shot.lateralDeviation ?: 0.0
                    center.x + (latDev.toFloat() / 20f) * radius
                } else {
                    val dev = shot.deviation ?: 0f
                    center.x + (dev / 2f) * radius
                }
                
                val y = if (shot.shotType == ShotType.FIELD) {
                    center.y
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
