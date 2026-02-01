package com.hrk.tracking.data.repository

import android.util.Log
import com.hrk.tracking.data.local.LocationDao
import com.hrk.tracking.data.local.LocationEntity
import com.hrk.tracking.util.LogTags


/**
 * Created by Ritik on: 10/01/26
 */

class LocationRepository(private val dao: LocationDao) {

    suspend fun save(location: LocationEntity): Boolean {
        return try {
            dao.insert(location)
            Log.d(LogTags.DB, "Inserted location into Room DB: lat=${location.latitude}, lng=${location.longitude}")
            true
        } catch (e: Exception) {
            Log.e(LogTags.DB, "Failed to insert location into Room DB", e)
            false
        }
    }

    suspend fun getAll() = dao.getAll()

    fun getCount() = dao.getCount()
}
