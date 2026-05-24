package com.ntu.electricity.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_config")
data class UserConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val studentId: String = "",
    val password: String = "",
    val campus: String = "",
    val building: String = "",
    val room: String = "",
    val rememberSelection: Boolean = true
)
