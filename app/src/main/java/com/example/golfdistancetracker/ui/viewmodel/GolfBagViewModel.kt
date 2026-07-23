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
            clubDao.insertClub(Club(name = name, type = type, number = number, brand = brand, model = model))
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
}
