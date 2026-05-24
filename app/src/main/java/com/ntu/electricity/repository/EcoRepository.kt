package com.ntu.electricity.repository

import com.ntu.electricity.data.local.UserPreferences
import com.ntu.electricity.data.local.dao.QueryHistoryDao
import com.ntu.electricity.data.local.entity.QueryHistoryEntity
import com.ntu.electricity.network.ElectricityQuerier
import com.ntu.electricity.network.NtuHttpClient
import com.ntu.electricity.network.RoomDataLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EcoRepository(
    private val queryHistoryDao: QueryHistoryDao,
    private val userPreferences: UserPreferences,
    private val httpClient: NtuHttpClient,
    private val roomDataLoader: RoomDataLoader
) {
    val queryHistory: Flow<List<QueryHistoryEntity>> = queryHistoryDao.getAllHistory()

    fun getCampusNames(): List<String> = roomDataLoader.getCampusNames()
    fun getBuildingNames(campus: String): List<String> = roomDataLoader.getBuildingNames(campus)
    fun getRoomNames(campus: String, building: String): List<String> =
        roomDataLoader.getRoomNames(campus, building)

    fun getCampusId(campus: String): String? = roomDataLoader.getCampusId(campus)
    fun getBuildingId(campus: String, building: String): String? =
        roomDataLoader.getBuildingId(campus, building)
    fun getRoomId(campus: String, building: String, room: String): String? =
        roomDataLoader.getRoomId(campus, building, room)

    suspend fun queryElectricity(
        studentId: String,
        password: String,
        campusId: String,
        buildingId: String,
        roomId: String
    ): ElectricityQuerier.QueryResult = withContext(Dispatchers.IO) {
        val querier = ElectricityQuerier(httpClient)
        querier.query(studentId, password, campusId, buildingId, roomId)
    }

    suspend fun saveQueryHistory(result: ElectricityQuerier.QueryResult, campus: String, building: String, room: String) {
        queryHistoryDao.insert(
            QueryHistoryEntity(
                date = result.date,
                time = result.time,
                electricity = result.electricity,
                campus = campus,
                building = building,
                room = room
            )
        )
    }

    suspend fun clearHistory() {
        queryHistoryDao.deleteAll()
    }

    fun saveUserConfig(
        studentId: String,
        password: String,
        campus: String,
        building: String,
        room: String,
        rememberSelection: Boolean
    ) {
        userPreferences.saveAll(studentId, password, campus, building, room, rememberSelection)
    }

    fun getUserConfig(): UserPreferences = userPreferences
}
