package com.example.golfdistancetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN isPar3 INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shots ADD COLUMN practiceSessionId TEXT")
            }
        }
    }
}
