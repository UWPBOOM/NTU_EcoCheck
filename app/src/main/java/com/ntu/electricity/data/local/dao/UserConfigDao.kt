package com.ntu.electricity.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ntu.electricity.data.local.entity.UserConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {
    @Query("SELECT * FROM user_config WHERE id = 1")
    fun getConfig(): Flow<UserConfigEntity?>

    @Query("SELECT * FROM user_config WHERE id = 1")
    suspend fun getConfigOnce(): UserConfigEntity?

    @Upsert
    suspend fun upsert(config: UserConfigEntity)
}
