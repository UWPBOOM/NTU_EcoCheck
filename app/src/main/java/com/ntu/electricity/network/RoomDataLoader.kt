package com.ntu.electricity.network

import com.ntu.electricity.data.model.RoomDataFile
import com.ntu.electricity.data.model.CampusData
import com.ntu.electricity.data.model.BuildingData
import android.content.Context
import kotlinx.serialization.json.Json

class RoomDataLoader(private val context: Context) {

    private var cachedData: RoomDataFile? = null

    fun load(): RoomDataFile {
        cachedData?.let { return it }
        val jsonStr = context.assets.open("ntu_electricity_rooms.json")
            .bufferedReader()
            .use { it.readText() }
        val json = Json { ignoreUnknownKeys = true }
        val data = json.decodeFromString<RoomDataFile>(jsonStr)
        cachedData = data
        return data
    }

    fun getCampusNames(): List<String> {
        return load().campusData.keys.toList()
    }

    fun getBuildingNames(campusName: String): List<String> {
        return load().campusData[campusName]?.buildings?.keys?.sortedBy {
            it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        } ?: emptyList()
    }

    fun getRoomNames(campusName: String, buildingName: String): List<String> {
        return load().campusData[campusName]?.buildings?.get(buildingName)?.rooms?.keys?.sortedBy {
            it.toIntOrNull() ?: 0
        } ?: emptyList()
    }

    fun getCampusId(campusName: String): String? {
        return load().campusData[campusName]?.id
    }

    fun getBuildingId(campusName: String, buildingName: String): String? {
        return load().campusData[campusName]?.buildings?.get(buildingName)?.id
    }

    fun getRoomId(campusName: String, buildingName: String, roomName: String): String? {
        return load().campusData[campusName]?.buildings?.get(buildingName)?.rooms?.get(roomName)
    }
}
