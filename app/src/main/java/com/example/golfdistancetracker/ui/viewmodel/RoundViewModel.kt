package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.*
import com.example.golfdistancetracker.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoundUiState(
    val currentRound: Round? = null,
    val allPlayers: List<Player> = emptyList(),
    val activePlayers: List<Player> = emptyList(),
    val selectedPlayer: Player? = null,
    val holeScores: List<HoleScore> = emptyList(),
    val courses: List<Course> = emptyList()
)

@HiltViewModel
class RoundViewModel @Inject constructor(
    private val roundDao: RoundDao,
    private val courseDao: CourseDao,
    private val playerDao: PlayerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoundUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            courseDao.getAllCourses().collect { list ->
                _uiState.update { it.copy(courses = list) }
            }
        }
        viewModelScope.launch {
            playerDao.getAllPlayers().collect { list ->
                _uiState.update { it.copy(allPlayers = list) }
            }
        }
    }

    fun startNewRound(course: Course) {
        viewModelScope.launch {
            val roundId = roundDao.insertRound(Round(courseId = course.id))
            val user = playerDao.getAllPlayers().first().find { it.isUser } ?: Player(id = 1, name = "Me", isUser = true)
            
            // Initialize holes for the main user
            for (i in 1..course.numberOfHoles) {
                roundDao.insertHoleScore(HoleScore(roundId = roundId, playerId = user.id, holeNumber = i))
            }
            loadRound(roundId)
            selectPlayer(user)
        }
    }

    fun addPlayerToRound(name: String) {
        val round = _uiState.value.currentRound ?: return
        viewModelScope.launch {
            val players = playerDao.getAllPlayers().first()
            var player = players.find { it.name == name }
            if (player == null) {
                val id = playerDao.insertPlayer(Player(name = name))
                player = Player(id = id, name = name)
            }
            
            val course = courseDao.getAllCourses().first().find { it.id == round.courseId } ?: return@launch
            
            // Initialize holes for the new player
            for (i in 1..course.numberOfHoles) {
                roundDao.insertHoleScore(HoleScore(roundId = round.id, playerId = player.id, holeNumber = i))
            }
            
            // Refresh
            loadRound(round.id)
        }
    }

    fun loadRound(roundId: Long) {
        viewModelScope.launch {
            val round = roundDao.getRoundById(roundId)
            combine(
                roundDao.getHoleScoresForRound(roundId),
                playerDao.getAllPlayers()
            ) { scores, allPlayers ->
                val playerIdsInRound = scores.map { it.playerId }.distinct()
                val activePlayers = allPlayers.filter { it.id in playerIdsInRound }
                
                _uiState.update { it.copy(
                    currentRound = round,
                    activePlayers = activePlayers,
                    holeScores = scores
                ) }
                
                if (_uiState.value.selectedPlayer == null) {
                    _uiState.update { it.copy(selectedPlayer = activePlayers.find { p -> p.isUser } ?: activePlayers.firstOrNull()) }
                }
            }.collect()
        }
    }

    fun selectPlayer(player: Player) {
        _uiState.update { it.copy(selectedPlayer = player) }
    }

    fun updateHoleScore(score: HoleScore) {
        viewModelScope.launch {
            roundDao.insertHoleScore(score)
        }
    }
}
