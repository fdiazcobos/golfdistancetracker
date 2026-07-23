package com.example.golfdistancetracker.data.dao

import androidx.room.*
import com.example.golfdistancetracker.data.entity.HoleScore
import com.example.golfdistancetracker.data.entity.Round
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds ORDER BY timestamp DESC")
    fun getAllRounds(): Flow<List<Round>>

    @Query("SELECT * FROM hole_scores WHERE roundId = :roundId ORDER BY holeNumber ASC")
    fun getHoleScoresForRound(roundId: Long): Flow<List<HoleScore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: Round): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoleScore(holeScore: HoleScore)

    @Update
    suspend fun updateRound(round: Round)

    @Delete
    suspend fun deleteRound(round: Round)
}
