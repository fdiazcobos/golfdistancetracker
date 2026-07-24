package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.golfdistancetracker.data.entity.HoleScore
import com.example.golfdistancetracker.data.entity.Player
import com.example.golfdistancetracker.ui.viewmodel.RoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(viewModel: RoundViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showCoursePicker by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.score_title)) },
                actions = {
                    if (uiState.currentRound != null) {
                        IconButton(onClick = { showLeaderboard = true }) {
                            Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard")
                        }
                        IconButton(onClick = { showAddPlayerDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Player")
                        }
                    }
                }
            ) 
        },
        floatingActionButton = {
            if (uiState.currentRound == null) {
                FloatingActionButton(onClick = { showCoursePicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.currentRound == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.score_empty))
                }
            } else {
                // Player Switcher
                PlayerSwitcher(
                    activePlayers = uiState.activePlayers,
                    selectedPlayer = uiState.selectedPlayer,
                    onPlayerSelected = { viewModel.selectPlayer(it) }
                )

                val currentPlayerScores = uiState.holeScores.filter { it.playerId == uiState.selectedPlayer?.id }
                
                Text(
                    "Total Strokes: ${currentPlayerScores.sumOf { it.strokes }}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentPlayerScores) { score ->
                        HoleScoreItem(score) { viewModel.updateHoleScore(it) }
                    }
                }
            }
        }

        if (showCoursePicker) {
            AlertDialog(
                onDismissRequest = { showCoursePicker = false },
                title = { Text(stringResource(R.string.score_select_course)) },
                text = {
                    Column {
                        uiState.courses.forEach { course ->
                            TextButton(onClick = {
                                viewModel.startNewRound(course)
                                showCoursePicker = false
                            }) {
                                Text(course.name)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showAddPlayerDialog) {
            AddPlayerDialog(
                onDismiss = { showAddPlayerDialog = false },
                onAdd = { name ->
                    viewModel.addPlayerToRound(name)
                    showAddPlayerDialog = false
                }
            )
        }

        if (showLeaderboard) {
            LeaderboardDialog(
                players = uiState.activePlayers,
                scores = uiState.holeScores,
                onDismiss = { showLeaderboard = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSwitcher(
    activePlayers: List<Player>,
    selectedPlayer: Player?,
    onPlayerSelected: (Player) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(activePlayers) { player ->
            FilterChip(
                selected = player == selectedPlayer,
                onClick = { onPlayerSelected(player) },
                label = { Text(player.name) },
                leadingIcon = {
                    if (player.isUser) Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend to Round") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Friend's Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if(name.isNotEmpty()) onAdd(name) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LeaderboardDialog(players: List<Player>, scores: List<HoleScore>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leaderboard", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                players.sortedBy { player ->
                    scores.filter { it.playerId == player.id }.sumOf { it.strokes }
                }.forEach { player ->
                    val total = scores.filter { it.playerId == player.id }.sumOf { it.strokes }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(player.name, style = MaterialTheme.typography.bodyLarge)
                        Text("$total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun HoleScoreItem(score: HoleScore, onUpdate: (HoleScore) -> Unit) {
    // Basic logic assuming par 4 if not known
    val par = 4 
    val diff = score.strokes - par
    val label = if (score.strokes > 0) {
        when {
            score.strokes == 1 -> "Hole in One!"
            diff <= -2 -> stringResource(R.string.score_eagle)
            diff == -1 -> stringResource(R.string.score_birdie)
            diff == 0 -> stringResource(R.string.score_par_label)
            diff == 1 -> stringResource(R.string.score_bogey)
            diff >= 2 -> stringResource(R.string.score_double_bogey)
            else -> ""
        }
    } else ""

    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.course_hole_title, score.holeNumber), fontWeight = FontWeight.Bold)
                if (label.isNotEmpty()) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if(score.strokes > 0) onUpdate(score.copy(strokes = score.strokes - 1)) }) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }
                Text("${score.strokes}", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onUpdate(score.copy(strokes = score.strokes + 1)) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}
