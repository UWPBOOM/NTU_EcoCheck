package com.ntu.electricity.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntu.electricity.EcoCheckApplication
import com.ntu.electricity.repository.EcoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EcoCheckApplication
    private val repository = EcoRepository(
        queryHistoryDao = app.database.queryHistoryDao(),
        userPreferences = app.userPreferences,
        httpClient = app.httpClient,
        roomDataLoader = app.roomDataLoader
    )

    private val _studentId = MutableStateFlow("")
    val studentId: StateFlow<String> = _studentId.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _selectedCampus = MutableStateFlow("")
    val selectedCampus: StateFlow<String> = _selectedCampus.asStateFlow()

    private val _selectedBuilding = MutableStateFlow("")
    val selectedBuilding: StateFlow<String> = _selectedBuilding.asStateFlow()

    private val _selectedRoom = MutableStateFlow("")
    val selectedRoom: StateFlow<String> = _selectedRoom.asStateFlow()

    private val _rememberSelection = MutableStateFlow(true)
    val rememberSelection: StateFlow<Boolean> = _rememberSelection.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResultUi?>(null)
    val queryResult: StateFlow<QueryResultUi?> = _queryResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val campusNames = repository.getCampusNames()

    init {
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        val config = repository.getUserConfig()
        config.logCurrentState()
        _studentId.value = config.studentId
        _password.value = config.password
        _rememberSelection.value = config.rememberSelection
        // Always load saved selections
        _selectedCampus.value = config.campus
        _selectedBuilding.value = config.building
        _selectedRoom.value = config.room
        Log.d("HomeVM", "loadSavedConfig: sid=${_studentId.value}, campus=${_selectedCampus.value}")
    }

    fun getCampusNames(): List<String> = campusNames

    fun getCampusId(campus: String): String? = repository.getCampusId(campus)
    fun getBuildingId(campus: String, building: String): String? = repository.getBuildingId(campus, building)
    fun getRoomId(campus: String, building: String, room: String): String? = repository.getRoomId(campus, building, room)

    fun getBuildingNames(): List<String> {
        return if (_selectedCampus.value.isNotEmpty()) {
            repository.getBuildingNames(_selectedCampus.value)
        } else emptyList()
    }

    fun getRoomNames(): List<String> {
        return if (_selectedCampus.value.isNotEmpty() && _selectedBuilding.value.isNotEmpty()) {
            repository.getRoomNames(_selectedCampus.value, _selectedBuilding.value)
        } else emptyList()
    }

    fun onStudentIdChange(value: String) { _studentId.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
    fun onRememberSelectionChange(value: Boolean) { _rememberSelection.value = value }

    fun onCampusSelected(campus: String) {
        _selectedCampus.value = campus
        _selectedBuilding.value = ""
        _selectedRoom.value = ""
    }

    fun onBuildingSelected(building: String) {
        _selectedBuilding.value = building
        _selectedRoom.value = ""
    }

    fun onRoomSelected(room: String) {
        _selectedRoom.value = room
    }

    fun saveConfig(sid: String, pwd: String, campus: String, building: String, room: String, remember: Boolean) {
        repository.saveUserConfig(sid, pwd, campus, building, room, remember)
    }

    fun query() {
        val sid = _studentId.value.trim()
        val pwd = _password.value.trim()
        val campus = _selectedCampus.value
        val building = _selectedBuilding.value
        val room = _selectedRoom.value

        if (sid.isEmpty() || pwd.isEmpty() || campus.isEmpty() || building.isEmpty() || room.isEmpty()) {
            _errorMessage.value = "请填写完整信息"
            return
        }

        val campusId = repository.getCampusId(campus)
        val buildingId = repository.getBuildingId(campus, building)
        val roomId = repository.getRoomId(campus, building, room)

        if (campusId == null || buildingId == null || roomId == null) {
            _errorMessage.value = "服务器繁忙，请稍后再试"
            return
        }

        _isLoading.value = true
        _queryResult.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.queryElectricity(sid, pwd, campusId, buildingId, roomId)
                _queryResult.value = QueryResultUi(
                    date = result.date,
                    time = result.time,
                    electricity = result.electricity
                )
                repository.saveQueryHistory(result, campus, building, room)
            } catch (e: Exception) {
                _errorMessage.value = "错误: ${e.javaClass.simpleName}: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

        // Always save config immediately (not dependent on query success)
        Log.d("HomeVM", "Saving config: sid=$sid, campus=$campus, remember=${_rememberSelection.value}")
        repository.saveUserConfig(
            sid, pwd, campus, building, room,
            _rememberSelection.value
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

data class QueryResultUi(
    val date: String,
    val time: String,
    val electricity: String
)
