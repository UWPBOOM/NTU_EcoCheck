package com.ntu.electricity.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "query_history")
data class QueryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val time: String,
    val electricity: String,
    val campus: String,
    val building: String,
    val room: String,
    val timestamp: Long = System.currentTimeMillis()
)
