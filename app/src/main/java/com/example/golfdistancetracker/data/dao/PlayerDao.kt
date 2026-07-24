package com.example.golfdistancetracker.data.dao

import androidx.room.*
import com.example.golfdistancetracker.data.entity.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players")
    fun getAllPlayers(): Flow<List<Player>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Delete
    suspend fun deletePlayer(player: Player)
}
