package com.example.golfdistancetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.CourseDao
import com.example.golfdistancetracker.data.dao.RoundDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.*

@Database(
    entities = [Club::class, Shot::class, Course::class, Hole::class, Round::class, HoleScore::class], 
    version = 7, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GolfDatabase : RoomDatabase() {
    abstract fun clubDao(): ClubDao
    abstract fun shotDao(): ShotDao
    abstract fun courseDao(): CourseDao
    abstract fun roundDao(): RoundDao
}
