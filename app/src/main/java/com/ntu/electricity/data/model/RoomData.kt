package com.ntu.electricity.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomDataFile(
    @SerialName("校区数据")
    val campusData: Map<String, CampusData>
)

@Serializable
data class CampusData(
    val id: String,
    val buildings: Map<String, BuildingData>
)

@Serializable
data class BuildingData(
    val id: String,
    val rooms: Map<String, String>
)
