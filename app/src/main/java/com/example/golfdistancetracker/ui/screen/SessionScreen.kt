package com.example.golfdistancetracker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    val selectedClub = uiState.selectedClub
                    Text(
                        selectedClub?.let { stringResource(R.string.session_tracking, it.name) } 
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
            val currentClub = uiState.selectedClub
            if (currentClub == null) {
                ClubSelectionGrid(clubs, uiState) { viewModel.selectClub(it) }
            } else if (currentClub.type == "Putter") {
                PutterManualEntryUI(
                    uiState = uiState,
                    onSave = { dist, dev, qual -> viewModel.saveManualShot(dist, dev, qual) },
                    onCancel = { viewModel.resetSession() }
                )
            } else {
                TrackingUI(
                    uiState = uiState,
                    onMarkStart = { viewModel.markStart() },
                    onMarkEnd = { viewModel.markEnd() },
                    onTargetChange = { viewModel.updateTargetDistance(it) },
                    onLockTarget = { viewModel.lockTargetHeading() },
                    onClearTarget = { viewModel.clearLockedHeading() }
                )
            }

            if (uiState.showShotSummary) {
                ShotSummaryDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.closeSummary() }
                )
            }
        }
    }
}

@Composable
fun WeatherWidget(weather: com.example.golfdistancetracker.ui.viewmodel.WeatherInfo) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        Column(horizontalAlignment = Alignment.End) {
            Text("${weather.temp.toInt()}°C", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${weather.windSpeed.toInt()} km/h", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(4.dp))
                // Wind Arrow relative to NORTH
                Icon(
                    imageVector = Icons.Default.Navigation, 
                    contentDescription = null, 
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(weather.windDeg.toFloat()),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ClubSelectionGrid(
    clubs: List<Club>, 
    uiState: com.example.golfdistancetracker.ui.viewmodel.SessionUiState,
    onSelect: (Club) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.session_select_club), style = MaterialTheme.typography.headlineSmall)
            Column(horizontalAlignment = Alignment.End) {
                Text("TODAY", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("${uiState.dailyTotalShots}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
        
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
                val iconRes = when {
                    club.type.contains("Driver", true) -> R.drawable.ic_club_driver
                    club.type.contains("Putter", true) -> R.drawable.ic_club_putter
                    club.type.contains("Wedge", true) -> R.drawable.ic_club_wedge
                    club.type.contains("Hibrido", true) -> R.drawable.ic_club_hybrid
                    else -> R.drawable.ic_club_iron
                }
                
                Button(
                    onClick = { onSelect(club) },
                    modifier = Modifier.height(80.dp),
                    colors = if (club == uiState.recommendedClub) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(iconRes), null, modifier = Modifier.size(24.dp))
                        Text(club.name, maxLines = 1, style = MaterialTheme.typography.labelMedium)
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
    onTargetChange: (Double?) -> Unit,
    onLockTarget: () -> Unit,
    onClearTarget: () -> Unit
) {
    var targetInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Distance & Plays Like
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val unitSuffix = if(uiState.distanceUnit == com.example.golfdistancetracker.data.prefs.DistanceUnit.YARDS) "yd" else "m"
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { 
                        targetInput = it
                        onTargetChange(it.toDoubleOrNull())
                    },
                    label = { Text(stringResource(R.string.session_target_dist, unitSuffix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (uiState.targetDistanceMeters != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Plays Like", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                UnitConverter.formatDistance(uiState.playsLikeDistance ?: uiState.targetDistanceMeters, uiState.distanceUnit),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        if (uiState.playsLikeAdjustmentMeters != 0.0) {
                            val adj = uiState.playsLikeAdjustmentMeters
                            Surface(
                                color = if (adj > 0) Color.Red.copy(alpha = 0.1f) else Color(0xFF2E7D32).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (adj > 0) "+${adj.toInt()}m wind/temp" else "${adj.toInt()}m wind",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (adj > 0) Color.Red else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compass & Target Locking
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    
                    // Wind direction arrow (relative to compass)
                    uiState.weather?.let { weather ->
                        rotate(degrees = weather.windDeg.toFloat()) {
                            drawLine(Color.Blue.copy(alpha = 0.3f), Offset(centerOffset.x, 0f), Offset(centerOffset.x, 40f), strokeWidth = 10f)
                        }
                    }

                    // Heading Arrow
                    rotate(degrees = uiState.currentHeading) {
                        val path = Path().apply {
                            moveTo(centerOffset.x, 10f)
                            lineTo(centerOffset.x - 15f, 40f)
                            lineTo(centerOffset.x + 15f, 40f)
                            close()
                        }
                        drawPath(path, color = Color.Red)
                        drawLine(color = Color.Red, start = centerOffset.copy(y = 40f), end = centerOffset.copy(y = size.height * 0.9f), strokeWidth = 6f)
                    }

                    // Locked Target Heading
                    uiState.lockedTargetHeading?.let { locked ->
                        rotate(degrees = locked) {
                            drawLine(Color(0xFF2E7D32), Offset(centerOffset.x, 0f), Offset(centerOffset.x, size.height), strokeWidth = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        }
                    }
                }
                Text("${uiState.currentHeading.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.lockedTargetHeading == null) {
                    Button(onClick = onLockTarget, contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Default.GpsFixed, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Aim to Flag", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(onClick = onClearTarget, contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Aim", fontSize = 12.sp)
                    }
                }
                Text("Point phone at flag\nto calculate wind", style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        // GPS & Action Buttons
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isGpsReady) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            )
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (uiState.isGpsReady) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, 
                    null, 
                    modifier = Modifier.size(16.dp),
                    tint = if (uiState.isGpsReady) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.isGpsReady) "GPS High Precision" else "Waiting for GPS...",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isGpsReady) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                )
            }
        }

        if (uiState.startLocation == null) {
            Button(
                onClick = onMarkStart,
                enabled = uiState.isGpsReady,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.session_mark_start), fontWeight = FontWeight.Bold)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onMarkEnd,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.session_mark_end), fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        AnimatedVisibility(visible = uiState.lastShotDistance != null) {
            Text(
                stringResource(R.string.session_distance, UnitConverter.formatDistance(uiState.lastShotDistance ?: 0.0, uiState.distanceUnit)),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PutterManualEntryUI(
    uiState: com.example.golfdistancetracker.ui.viewmodel.SessionUiState,
    onSave: (Double, Double, Int) -> Unit,
    onCancel: () -> Unit
) {
    var dist by remember { mutableStateOf("") }
    var dev by remember { mutableStateOf(0f) }
    var qual by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Putter Result", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = dist,
            onValueChange = { dist = it },
            label = { Text("Distance (${if(uiState.distanceUnit == com.example.golfdistancetracker.data.prefs.DistanceUnit.YARDS) "yd" else "m"})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            Text("Lateral Deviation: ${dev.toInt()}m", style = MaterialTheme.typography.titleMedium)
            Slider(value = dev, onValueChange = { dev = it }, valueRange = -10f..10f, steps = 19)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Left", style = MaterialTheme.typography.labelSmall)
                Text("Center", style = MaterialTheme.typography.labelSmall)
                Text("Right", style = MaterialTheme.typography.labelSmall)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Quality", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Short", "Good", "Long").forEachIndexed { index, label ->
                    Button(
                        onClick = { qual = index },
                        colors = if (qual == index) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                    ) { Text(label) }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onSave(dist.toDoubleOrNull() ?: 0.0, dev.toDouble(), qual) },
                modifier = Modifier.weight(1f),
                enabled = dist.isNotEmpty()
            ) { Text("Save Putter") }
        }
    }
}

@Composable
fun ShotSummaryDialog(
    uiState: com.example.golfdistancetracker.ui.viewmodel.SessionUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.session_summary_title)) 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Distance Section
                Column {
                    Text(
                        UnitConverter.formatDistance(uiState.lastShotDistance, uiState.distanceUnit),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    uiState.lastShotDistDiff?.let { diff ->
                        val isLonger = diff > 0
                        val color = if (isLonger) Color(0xFF2E7D32) else Color.Red
                        val label = if (isLonger) R.string.session_longer else R.string.session_shorter
                        
                        Text(
                            stringResource(label, UnitConverter.formatDistance(Math.abs(diff), uiState.distanceUnit)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    } ?: Text(stringResource(R.string.session_on_average), style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                // Direction Section
                Column {
                    val latDev = uiState.lastShotLatDev ?: 0.0
                    val isRight = latDev > 0
                    val sideLabel = if (isRight) R.string.session_right else R.string.session_left
                    
                    Text(
                        stringResource(R.string.session_dev_side, UnitConverter.formatDistance(Math.abs(latDev), uiState.distanceUnit), stringResource(sideLabel)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    uiState.lastShotAngleDev?.let { angle ->
                        Text(
                            stringResource(R.string.session_dev_angle, Math.abs(angle)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Visual Deviation Bar
                Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                        drawLine(color = Color.LightGray, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = 4f)
                        drawLine(color = Color.Gray, start = Offset(size.width/2, 0f), end = Offset(size.width/2, size.height), strokeWidth = 4f)
                    }
                    
                    val maxDev = 20.0 
                    val progress = ((uiState.lastShotLatDev ?: 0.0) / maxDev).coerceIn(-1.0, 1.0).toFloat()
                    
                    Box(
                        modifier = Modifier
                            .offset(x = (80 * progress).dp) 
                            .size(12.dp)
                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}
