package com.example.golfdistancetracker.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.*
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearPermissionGuard {
                GolfWearApp()
            }
        }
    }
}

@Composable
fun WearPermissionGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.VIBRATE
    )

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all { 
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    if (permissionsGranted) {
        content()
    } else {
        AppScaffold {
            ScreenScaffold {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(requiredPermissions) }
                    ) {
                        Text("Grant")
                    }
                }
            }
        }
    }
}

@Composable
fun GolfWearApp(viewModel: WearViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    AppScaffold {
        when (uiState.screen) {
            WearScreen.MODE_SELECTION -> ModeSelectionScreen(
                uiState = uiState,
                onModeSelected = { viewModel.selectMode(it) },
                onOpenSettings = { viewModel.openSettings() }
            )
            WearScreen.CLUB_SELECTION -> ClubSelectionScreen(
                uiState = uiState,
                onClubSelected = { viewModel.selectClub(it) },
                onBack = { viewModel.resetToStart() }
            )
            WearScreen.READY_TO_HIT -> ReadyToHitScreen(
                uiState = uiState,
                onManualMark = { viewModel.manualMarkShot() },
                onBack = { viewModel.resetToStart() }
            )
            WearScreen.WALKING -> WalkingScreen(
                uiState = uiState,
                onReachedBall = { viewModel.reachedBall() },
                onBack = { viewModel.resetToStart() }
            )
            WearScreen.DIRECTION_INPUT -> DirectionPickerScreen(
                onDirectionSelected = { viewModel.selectDirection(it) },
                onBack = { viewModel.resetToStart() }
            )
            WearScreen.PRACTICE_RATING -> PracticeRatingScreen(
                onRated = { viewModel.ratePracticeShot(it) }
            )
            WearScreen.SUMMARY -> SummaryScreen(
                uiState = uiState,
                onDone = { viewModel.resetToStart() }
            )
            WearScreen.SETTINGS -> WearSettingsScreen(
                uiState = uiState,
                onUpdateAuto = { viewModel.updateAutoImpact(it) },
                onUpdateThreshold = { viewModel.updateImpactThreshold(it) },
                onUpdateGps = { viewModel.updateGpsSource(it) },
                onBack = { viewModel.resetToStart() }
            )
        }
    }
}

@Composable
fun ModeSelectionScreen(uiState: WearUiState, onModeSelected: (WearMode) -> Unit, onOpenSettings: () -> Unit) {
    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Connectivity indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if(uiState.isPhoneAppActive) Color.Green else Color.Gray, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if(uiState.isPhoneAppActive) "LINKED" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp,
                    color = if(uiState.isPhoneAppActive) Color.Green else Color.Gray
                )
            }
            
            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(0.85f).height(52.dp),
                onClick = { onModeSelected(WearMode.PLAY) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GolfCourse, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PLAY")
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(0.85f).height(52.dp),
                onClick = { onModeSelected(WearMode.PRACTICE) },
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsGolf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PRACTICE")
                }
            }
            
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp).padding(top = 4.dp)) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun WearSettingsScreen(
    uiState: WearUiState,
    onUpdateAuto: (Boolean) -> Unit,
    onUpdateThreshold: (Float) -> Unit,
    onUpdateGps: (String) -> Unit,
    onBack: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = columnState, timeText = { TimeText() }) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Settings", modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
            item {
                Button(
                    onClick = { onUpdateAuto(!uiState.autoImpactEnabled) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = if (uiState.autoImpactEnabled) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(if (uiState.autoImpactEnabled) "Auto Detect: ON" else "Auto Detect: OFF", fontSize = 10.sp)
                }
            }
            item {
                Text("GPS Source", modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { onUpdateGps("Phone") },
                        colors = if (uiState.gpsSource == "Phone") ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Phone", fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { onUpdateGps("Watch") },
                        colors = if (uiState.gpsSource == "Watch") ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Watch", fontSize = 10.sp)
                    }
                }
            }
            
            if (uiState.autoImpactEnabled) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text("Impact: ${uiState.impactThreshold.toInt()}G", style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Slider(
                            value = uiState.impactThreshold,
                            onValueChange = onUpdateThreshold,
                            valueRange = 15f..100f,
                            steps = 8
                        )
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("v0.3.0", style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.settings_build_date), style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            item {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun ClubSelectionScreen(uiState: WearUiState, onClubSelected: (String) -> Unit, onBack: () -> Unit) {
    val columnState = rememberTransformingLazyColumnState()
    val clubs = listOf("Driver", "3 Wood", "4 Iron", "5 Iron", "6 Iron", "7 Iron", "8 Iron", "9 Iron", "PW", "SW", "LW", "Putter")

    ScreenScaffold(scrollState = columnState, timeText = { TimeText() }) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "Select Club",
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            items(clubs.size) { index ->
                val clubName = clubs[index]
                val usage = uiState.clubUsageMap[clubName] ?: 0
                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
                    onClick = { onClubSelected(clubName) },
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(clubName, fontSize = 14.sp)
                        if (usage > 0) {
                            Text("($usage today)", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ReadyToHitScreen(uiState: WearUiState, onManualMark: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(top = 12.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Club & Daily Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    uiState.currentClub.uppercase(), 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "TOTAL: ${uiState.dailyTotal}", 
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            // Ready Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("READY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (uiState.autoImpactEnabled) "Vibration ON" else "Auto OFF", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.autoImpactEnabled) Color(0xFF4CAF50) else Color.Gray
                )
            }

            // Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) { 
                        Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.8f)) 
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onManualMark, modifier = Modifier.height(48.dp).weight(1f)) { 
                        Text("MARK", fontWeight = FontWeight.Black) 
                    }
                }
                
                // Status Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        if (uiState.isUsingPhoneGps) Icons.Default.Smartphone else Icons.Default.Watch, 
                        null, 
                        modifier = Modifier.size(10.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (uiState.isUsingPhoneGps) "Phone GPS" else "Watch GPS", fontSize = 8.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun WalkingScreen(uiState: WearUiState, onReachedBall: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp)) 
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "WALKING", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${uiState.currentShotDistance?.toInt() ?: 0}m",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )
            }

            Button(modifier = Modifier.fillMaxWidth().height(48.dp), onClick = onReachedBall) {
                Text("FOUND BALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DirectionPickerScreen(onDirectionSelected: (String) -> Unit, onBack: () -> Unit) {
    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp)) 
                }
                Spacer(Modifier.weight(1f))
                Text("Direction", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1.3f))
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onDirectionSelected("Left") }, modifier = Modifier.size(52.dp), colors = ButtonDefaults.filledTonalButtonColors()) { Text("⬅️", fontSize = 24.sp) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onDirectionSelected("Straight") }, modifier = Modifier.size(60.dp)) { Text("🎯", fontSize = 28.sp) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onDirectionSelected("Right") }, modifier = Modifier.size(52.dp), colors = ButtonDefaults.filledTonalButtonColors()) { Text("➡️", fontSize = 24.sp) }
            }
        }
    }
}

@Composable
fun PracticeRatingScreen(onRated: (Int) -> Unit) {
    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("How was it?", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.Center, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onRated(0) }, 
                    modifier = Modifier.size(46.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.9f))
                ) { 
                    Text("💩", fontSize = 24.sp) 
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onRated(1) }, 
                    modifier = Modifier.size(54.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { 
                    Text("👍", fontSize = 26.sp) 
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onRated(2) }, 
                    modifier = Modifier.size(46.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { 
                    Text("🔥", fontSize = 24.sp) 
                }
            }
            }
        }
    }
}

@Composable
fun SummaryScreen(uiState: WearUiState, onDone: () -> Unit) {
    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SHOT SAVED", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4CAF50))
            Spacer(Modifier.height(12.dp))
            Text("${uiState.lastShotDistance?.toInt() ?: 0}m", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(uiState.lastShotDirection ?: "Center", style = MaterialTheme.typography.labelSmall)
            
            Spacer(Modifier.height(16.dp))
            Button(modifier = Modifier.fillMaxWidth().height(48.dp), onClick = onDone) { Text("DONE") }
        }
    }
}
