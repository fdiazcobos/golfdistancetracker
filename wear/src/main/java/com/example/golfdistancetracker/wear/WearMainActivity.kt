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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val criticalPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    val extraPermissions = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

    var permissionsGranted by remember {
        mutableStateOf(
            criticalPermissions.all { 
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsGranted = criticalPermissions.all { 
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = criticalPermissions.all { permissions[it] == true }
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
                        text = "Access Required",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Need GPS to track your shot distances.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(criticalPermissions + extraPermissions) }
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
        ScreenScaffold(
            timeText = { TimeText() }
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
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
    }
}

@Composable
fun ModeSelectionScreen(uiState: WearUiState, onModeSelected: (WearMode) -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if(uiState.isPhoneAppActive) Color.Green else Color.Gray, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if(uiState.isPhoneAppActive) "LINKED" else "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = if(uiState.isPhoneAppActive) Color.Green else Color.Gray
            )
        }
        
        Spacer(Modifier.height(12.dp))

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
        
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
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
    TransformingLazyColumn(
        state = columnState,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Settings", modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
        item {
            Button(
                onClick = { onUpdateAuto(!uiState.autoImpactEnabled) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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
                Text("v0.3.3", style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.settings_build_date), style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
                Text("Done")
            }
        }
    }
}

@Composable
fun ClubSelectionScreen(uiState: WearUiState, onClubSelected: (SyncedClub) -> Unit, onBack: () -> Unit) {
    val columnState = rememberTransformingLazyColumnState()
    val clubs = uiState.syncedClubs.ifEmpty { 
        listOf(SyncedClub(0, "Connecting...", "Iron"))
    }

    TransformingLazyColumn(
        state = columnState,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Select Club",
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
        items(clubs.size) { index ->
            val club = clubs[index]
            val usage = uiState.clubUsageMap[club.name] ?: 0
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
                onClick = { if(club.id != 0L) onClubSelected(club) },
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(club.name, fontSize = 14.sp)
                    if (usage > 0) {
                        Text("($usage today)", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ReadyToHitScreen(uiState: WearUiState, onManualMark: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 36.dp, bottom = 28.dp, start = 14.dp, end = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Club & Daily Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                uiState.currentClub?.name?.uppercase() ?: "CLUB", 
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(if (uiState.isUsingPhoneGps) Icons.Default.Smartphone else Icons.Default.Watch, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(if (uiState.isUsingPhoneGps) "Phone GPS" else "Watch GPS", fontSize = 8.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun WalkingScreen(uiState: WearUiState, onReachedBall: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 36.dp, bottom = 28.dp, start = 16.dp, end = 16.dp),
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

@Composable
fun DirectionPickerScreen(onDirectionSelected: (String) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 36.dp, bottom = 28.dp),
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
            Button(onClick = { onDirectionSelected("Left") }, modifier = Modifier.size(56.dp), colors = ButtonDefaults.filledTonalButtonColors()) { Text("⬅️", fontSize = 24.sp) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onDirectionSelected("Straight") }, modifier = Modifier.size(64.dp)) { Text("🎯", fontSize = 28.sp) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onDirectionSelected("Right") }, modifier = Modifier.size(56.dp), colors = ButtonDefaults.filledTonalButtonColors()) { Text("➡️", fontSize = 24.sp) }
        }
    }
}

@Composable
fun PracticeRatingScreen(onRated: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(top = 10.dp),
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
                    modifier = Modifier.size(52.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.9f))
                ) { 
                    Text("💩", fontSize = 28.sp) 
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { onRated(1) }, 
                    modifier = Modifier.size(60.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { 
                    Text("👍", fontSize = 30.sp) 
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { onRated(2) }, 
                    modifier = Modifier.size(52.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { 
                    Text("🔥", fontSize = 28.sp) 
                }
            }
        }
    }
}

@Composable
fun SummaryScreen(uiState: WearUiState, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
