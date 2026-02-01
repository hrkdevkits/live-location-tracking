package com.hrk.tracking.sdk

import com.hrk.tracking.data.local.LocationEntity

/**
 * Callback interface for handling synced location data.
 * 
 * Implement this interface to receive location data when syncData() is called.
 */
interface SyncCallback {
    /**
     * Called when location data is ready to be synced.
     * 
     * @param locations List of location entities to sync
     * @param timeInterval The time interval (in seconds) that was used for this sync
     */
    fun onSyncData(locations: List<LocationEntity>, timeInterval: Long)
    
    /**
     * Called when sync fails or no data is available.
     * 
     * @param error Error message or null if no data available
     */
    fun onSyncError(error: String?)
}
