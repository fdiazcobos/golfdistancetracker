package com.example.golfdistancetracker.data.dao

import androidx.room.*
import com.example.golfdistancetracker.data.entity.Course
import com.example.golfdistancetracker.data.entity.Hole
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM holes WHERE courseId = :courseId ORDER BY holeNumber ASC")
    fun getHolesForCourse(courseId: Long): Flow<List<Hole>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHole(hole: Hole)

    @Update
    suspend fun updateCourse(course: Course)

    @Update
    suspend fun updateHole(hole: Hole)

    @Delete
    suspend fun deleteCourse(course: Course)
}
