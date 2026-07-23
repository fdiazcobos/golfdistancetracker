package com.example.golfdistancetracker.data.dao

import androidx.room.*
import com.example.golfdistancetracker.data.entity.Club
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {
    @Query("SELECT * FROM clubs")
    fun getAllClubs(): Flow<List<Club>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: Club)

    @Update
    suspend fun updateClub(club: Club)

    @Delete
    suspend fun deleteClub(club: Club)
}
