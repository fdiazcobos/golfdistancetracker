package com.example.golfdistancetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.golfdistancetracker.data.dao.*
import com.example.golfdistancetracker.data.entity.*

@Database(
    entities = [Club::class, Shot::class, Course::class, Hole::class, Round::class, HoleScore::class, Player::class], 
    version = 8, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GolfDatabase : RoomDatabase() {
    abstract fun clubDao(): ClubDao
    abstract fun shotDao(): ShotDao
    abstract fun courseDao(): CourseDao
    abstract fun roundDao(): RoundDao
    abstract fun playerDao(): PlayerDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN isPar3 INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shots ADD COLUMN practiceSessionId TEXT")
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isUser` INTEGER NOT NULL)")
                // Insert default user
                db.execSQL("INSERT INTO players (id, name, isUser) VALUES (1, 'Me', 1)")
                // Add playerId column to hole_scores with default 1 (the user we just inserted)
                db.execSQL("ALTER TABLE hole_scores ADD COLUMN playerId INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
