package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.golfdistancetracker.R
import com.example.golfdistancetracker.data.entity.HoleScore
import com.example.golfdistancetracker.ui.viewmodel.RoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(viewModel: RoundViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showCoursePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.score_title)) }) },
        floatingActionButton = {
            if (uiState.holeScores.isEmpty()) {
                FloatingActionButton(onClick = { showCoursePicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.holeScores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.score_empty))
                }
            } else {
                Text(
                    stringResource(R.string.score_total, uiState.holeScores.sumOf { it.strokes }),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.holeScores) { score ->
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
    }
}

@Composable
fun HoleScoreItem(score: HoleScore, onUpdate: (HoleScore) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.course_hole_title, score.holeNumber), fontWeight = FontWeight.Bold)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if(score.strokes > 0) onUpdate(score.copy(strokes = score.strokes - 1)) }) {
                    Text("-", style = MaterialTheme.typography.headlineMedium)
                }
                Text("${score.strokes}", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onUpdate(score.copy(strokes = score.strokes + 1)) }) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
