package com.example.golfdistancetracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GolfWearApp()
        }
    }
}

@Composable
fun GolfWearApp(viewModel: WearViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    AppScaffold {
        when {
            !uiState.isTracking && uiState.lastTempo == null -> {
                ModeSelectionScreen(
                    onPlaySelected = { viewModel.setMode(WearMode.PLAY); viewModel.startSession() },
                    onPracticeSelected = { viewModel.setMode(WearMode.PRACTICE); viewModel.startSession() }
                )
            }
            else -> {
                TrackingScreen(
                    uiState = uiState,
                    onStop = { viewModel.stopSession() }
                )
            }
        }
    }
}

@Composable
fun ModeSelectionScreen(onPlaySelected: () -> Unit, onPracticeSelected: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = onPlaySelected,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("PLAY ROUND", style = MaterialTheme.typography.labelMedium)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = onPracticeSelected,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Text("PRACTICE", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun TrackingScreen(uiState: WearUiState, onStop: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Text(
                uiState.currentClub.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            // Central Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.mode == WearMode.PLAY && uiState.currentShotDistance != null) {
                    Text(
                        "${uiState.currentShotDistance.toInt()}m",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("WALKING...", style = MaterialTheme.typography.labelSmall)
                } else {
                    Text(
                        uiState.message,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                
                if (uiState.lastTempo != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("TEMPO: ${uiState.lastTempo}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                }
            }
            
            // Bottom Bar: Stop + GPS Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isUsingPhoneGps) Icons.Default.Smartphone else Icons.Default.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = if (uiState.isUsingPhoneGps) Color(0xFF4CAF50) else Color.LightGray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (uiState.isUsingPhoneGps) "Phone GPS" else "Watch GPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
