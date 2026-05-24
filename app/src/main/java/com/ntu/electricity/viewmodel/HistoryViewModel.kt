package com.ntu.electricity.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntu.electricity.EcoCheckApplication
import com.ntu.electricity.data.local.entity.QueryHistoryEntity
import com.ntu.electricity.repository.EcoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EcoCheckApplication
    private val repository = EcoRepository(
        queryHistoryDao = app.database.queryHistoryDao(),
        userPreferences = app.userPreferences,
        httpClient = app.httpClient,
        roomDataLoader = app.roomDataLoader
    )

    val history: StateFlow<List<QueryHistoryEntity>> = repository.queryHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
