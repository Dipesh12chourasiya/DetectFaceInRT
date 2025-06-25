package com.example.detectfaceandexpression.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.detectfaceandexpression.roomDB.AppDatabase
import com.example.detectfaceandexpression.roomDB.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application): AndroidViewModel(application) {

    // Room data base
    private val sessionDao = AppDatabase.getDatabase(application).sessionDao()

    fun saveSession(session: SessionEntity) {
        viewModelScope.launch {
            sessionDao.insertSession(session)
        }
    }

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            sessionDao.deleteSession(session)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            sessionDao.deleteAll()
        }
    }

}
