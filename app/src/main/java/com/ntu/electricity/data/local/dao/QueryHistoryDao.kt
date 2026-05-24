package com.ntu.electricity.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ntu.electricity.data.local.entity.QueryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryHistoryDao {
    @Query("SELECT * FROM query_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<QueryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: QueryHistoryEntity)

    @Query("DELETE FROM query_history")
    suspend fun deleteAll()
}
