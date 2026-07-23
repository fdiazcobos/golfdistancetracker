package com.example.golfdistancetracker.data.dao

import androidx.room.*
import com.example.golfdistancetracker.data.entity.Shot
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots")
    fun getAllShots(): Flow<List<Shot>>

    @Query("SELECT * FROM shots WHERE clubId = :clubId")
    fun getShotsForClub(clubId: Long): Flow<List<Shot>>

    @Query("SELECT AVG(distance) FROM shots WHERE clubId = :clubId")
    fun getAverageDistanceForClub(clubId: Long): Flow<Double?>

    @Insert
    suspend fun insertShot(shot: Shot)

    @Query("DELETE FROM shots")
    suspend fun deleteAllShots()
}
