package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.entity.Club
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GolfBagViewModel @Inject constructor(
    private val clubDao: ClubDao
) : ViewModel() {
    val clubs = clubDao.getAllClubs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addClub(name: String, type: String, number: String?, brand: String?, model: String?) {
        viewModelScope.launch {
            val currentMax = clubs.value.maxOfOrNull { it.displayOrder } ?: -1
            clubDao.insertClub(Club(name = name, type = type, number = number, brand = brand, model = model, displayOrder = currentMax + 1))
        }
    }

    fun deleteClub(club: Club) {
        viewModelScope.launch {
            clubDao.deleteClub(club)
        }
    }

    fun updateClub(club: Club) {
        viewModelScope.launch {
            clubDao.updateClub(club)
        }
    }

    fun moveClubUp(club: Club) {
        val list = clubs.value
        val index = list.indexOf(club)
        if (index > 0) {
            reorderAll(club, index - 1)
        }
    }

    fun moveClubDown(club: Club) {
        val list = clubs.value
        val index = list.indexOf(club)
        if (index != -1 && index < list.size - 1) {
            reorderAll(club, index + 1)
        }
    }

    private fun reorderAll(targetClub: Club, newIndex: Int) {
        viewModelScope.launch {
            val listWithoutTarget = clubs.value.filter { it.id != targetClub.id }
            val newList = listWithoutTarget.toMutableList()
            newList.add(newIndex.coerceIn(0, newList.size), targetClub)
            newList.forEachIndexed { i, c ->
                clubDao.updateClub(c.copy(displayOrder = i))
            }
        }
    }
}
