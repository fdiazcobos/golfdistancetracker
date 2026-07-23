package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(statsViewModel: StatsViewModel, onNavigate: (String) -> Unit) {
    val stats by statsViewModel.stats.collectAsState()
    
    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.dash_title), fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = null) 
                    }
                }
            ) 
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1535131749006-b7f58c99034b?q=80&w=1000&auto=format&fit=crop",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                        ) {
                            Text(
                                stringResource(R.string.dash_hero_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                stringResource(R.string.dash_hero_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.dash_management), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardActionButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("courses") },
                        icon = Icons.Default.GolfCourse,
                        label = stringResource(R.string.course_title),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    DashboardActionButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("scorecard") },
                        icon = Icons.Default.Description,
                        label = stringResource(R.string.score_title),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }

            item {
                Text(stringResource(R.string.dash_quick_stats), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                val largeGaps = stats.filter { (it.gapToNext ?: 0.0) > 15.0 }
                if (largeGaps.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.dash_gap_alert, largeGaps.first().club.name),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val avgAccuracy = stats.map { it.accuracyPct }.average().takeIf { !it.isNaN() } ?: 0.0
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_accuracy),
                        value = "${(avgAccuracy * 100).toInt()}%",
                        icon = Icons.Default.GpsFixed,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                    val bestClub = stats.maxByOrNull { it.averageDistance ?: 0.0 }
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dash_power),
                        value = bestClub?.club?.name ?: stringResource(R.string.stats_no_data),
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            item {
                Text(stringResource(R.string.dash_tips), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.dash_did_you_know), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.dash_tip_aim), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionButton(modifier: Modifier, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DashboardStatCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}
