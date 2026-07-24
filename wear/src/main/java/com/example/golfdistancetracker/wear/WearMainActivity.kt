package com.example.golfdistancetracker.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
        Manifest.permission.ACTIVITY_RECOGNITION
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
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.perm_title),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.perm_desc),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(requiredPermissions) }
                    ) {
                        Text(stringResource(R.string.common_grant))
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
                onUpdateGps = { viewModel.updateGpsSource(it) },
                onBack = { viewModel.resetToStart() }
            )
        }
    }
}

@Composable
fun ModeSelectionScreen(onModeSelected: (WearMode) -> Unit, onOpenSettings: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = { onModeSelected(WearMode.PLAY) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GolfCourse, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PLAY", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            Button(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = { onModeSelected(WearMode.PRACTICE) },
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsGolf, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PRACTICE", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp).padding(top = 4.dp)) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun WearSettingsScreen(
    uiState: WearUiState,
    onUpdateAuto: (Boolean) -> Unit,
    onUpdateGps: (String) -> Unit,
    onBack: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Settings", modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
            item {
                Button(
                    onClick = { onUpdateAuto(!uiState.autoImpactEnabled) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = if (uiState.autoImpactEnabled) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(if (uiState.autoImpactEnabled) "Auto Detect: ON" else "Auto Detect: OFF", fontSize = 10.sp)
                }
            }
            item {
                Text("GPS Source", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { onUpdateGps("Phone") },
                        colors = if (uiState.gpsSource == "Phone") ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Phone", fontSize = 8.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { onUpdateGps("Watch") },
                        colors = if (uiState.gpsSource == "Watch") ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Watch", fontSize = 8.sp)
                    }
                }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("v0.2.1", style = MaterialTheme.typography.labelSmall)
                    Text("Build: July 24, 2026", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            item {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun ClubSelectionScreen(uiState: WearUiState, onClubSelected: (String) -> Unit, onBack: () -> Unit) {
    val columnState = rememberTransformingLazyColumnState()
    val clubs = listOf("Driver", "3 Wood", "4 Iron", "5 Iron", "6 Iron", "7 Iron", "8 Iron", "9 Iron", "PW", "SW", "LW", "Putter")

    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "Select Club",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            items(clubs.size) { index ->
                val clubName = clubs[index]
                val usage = uiState.clubUsageMap[clubName] ?: 0
                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    onClick = { onClubSelected(clubName) },
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(clubName, fontSize = 12.sp)
                        if (usage > 0) {
                            Text("($usage today)", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ReadyToHitScreen(uiState: WearUiState, onManualMark: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(uiState.currentClub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Total: ${uiState.dailyTotal}", style = MaterialTheme.typography.labelSmall)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("READY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                if (uiState.autoImpactEnabled) {
                    Text("Swing or tap mark", style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("Auto-detect OFF", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                Button(
                    onClick = { onManualMark() }, 
                    modifier = Modifier.height(48.dp).weight(1f)
                ) { 
                    Text("MARK", fontWeight = FontWeight.Bold) 
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (uiState.isUsingPhoneGps) Icons.Default.Smartphone else Icons.Default.Watch, null, modifier = Modifier.size(8.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (uiState.isUsingPhoneGps) "Phone GPS" else "Watch GPS", fontSize = 7.sp)
            }
        }
    }
}

@Composable
fun WalkingScreen(uiState: WearUiState, onReachedBall: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp)) 
                }
                Text("WALKING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(24.dp))
            }
            
            Text(
                "${uiState.currentShotDistance?.toInt() ?: 0}m",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )

            Button(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                onClick = onReachedBall
            ) {
                Text("FOUND BALL", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DirectionPickerScreen(onDirectionSelected: (String) -> Unit, onBack: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp)) 
                }
                Spacer(Modifier.weight(1f))
                Text("Direction", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1.3f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { onDirectionSelected("Left") }, modifier = Modifier.size(44.dp), colors = ButtonDefaults.filledTonalButtonColors()) { Text("L") }
                Button(onClick = { onDirectionSelected("Straight") }, modifier = Modifier.size(52.dp)) { Text("C") }
                Button(onClick = { onDirectionSelected("Right") }, modifier = Modifier.size(44.dp), colors = ButtonDefaults.filledTonalButtonColors()) { Text("R") }
            }
        }
    }
}

@Composable
fun PracticeRatingScreen(onRated: (Int) -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("How was it?", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { onRated(0) }, modifier = Modifier.weight(1f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("MISS", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = { onRated(1) }, modifier = Modifier.weight(1f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("GOOD", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = { onRated(2) }, modifier = Modifier.weight(1f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("GREAT", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun SummaryScreen(uiState: WearUiState, onDone: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SHOT SAVED", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4CAF50))
            Spacer(Modifier.height(8.dp))
            Text("${uiState.lastShotDistance?.toInt() ?: 0}m", style = MaterialTheme.typography.titleLarge)
            Text(uiState.lastShotDirection ?: "Center", style = MaterialTheme.typography.labelSmall)
            
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("DONE") }
        }
    }
}
