package com.hrk.tracking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


/**
 * Created by Ritik on: 10/01/26
 */

@Dao
interface LocationDao {

    @Insert
    suspend fun insert(location: LocationEntity)

    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT COUNT(*) FROM location_logs")
    fun getCount(): Flow<Int>
}
