package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.CourseDao
import com.example.golfdistancetracker.data.entity.Course
import com.example.golfdistancetracker.data.entity.Hole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseDao: CourseDao
) : ViewModel() {
    val courses = courseDao.getAllCourses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCourse(name: String, location: String?, holes: Int, isPar3: Boolean) {
        viewModelScope.launch {
            val courseId = courseDao.insertCourse(Course(name = name, location = location, numberOfHoles = holes, isPar3 = isPar3))
            val defaultPar = if (isPar3) 3 else 4
            for (i in 1..holes) {
                courseDao.insertHole(Hole(courseId = courseId, holeNumber = i, par = defaultPar))
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            courseDao.deleteCourse(course)
        }
    }

    fun getHoles(courseId: Long) = courseDao.getHolesForCourse(courseId)

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            courseDao.updateCourse(course)
        }
    }

    fun updateHole(hole: Hole) {
        viewModelScope.launch {
            courseDao.updateHole(hole)
        }
    }
}
