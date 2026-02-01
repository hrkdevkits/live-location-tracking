package com.hrk.tracking.sdk

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.hrk.tracking.data.local.AppDatabase
import com.hrk.tracking.data.repository.LocationRepository
import com.hrk.tracking.service.LocationTrackingService
import com.hrk.tracking.util.BatteryOptimizationHelper
import com.hrk.tracking.util.LogTags
import com.hrk.tracking.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Live Tracking SDK
 * 
 * Simple SDK for tracking location in foreground and background.
 * 
 * Usage:
 * ```kotlin
 * // Start tracking with default interval (10 seconds)
 * LiveTrackingSDK.startTracking(context)
 * 
 * // Start tracking with custom interval (30 seconds)
 * LiveTrackingSDK.startTracking(context, intervalSeconds = 30)
 * 
 * // Stop tracking
 * LiveTrackingSDK.stopTracking(context)
 * 
 * // Sync data with callback
 * LiveTrackingSDK.syncData(context, time = 60, callback)
 * ```
 */
object LiveTrackingSDK {
    
    private const val DEFAULT_INTERVAL_SECONDS = 10L
    private const val EXTRA_INTERVAL_MS = "interval_ms"
    
    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Start location tracking service with default interval (10 seconds).
     * This will log current location in both foreground and background.
     * 
     * @param context Application context
     * @return true if tracking started successfully, false otherwise
     */
    @JvmStatic
    fun startTracking(context: Context): Boolean {
        return startTracking(context, DEFAULT_INTERVAL_SECONDS)
    }

    /**
     * Start location tracking service with custom interval.
     * This will log current location in both foreground and background.
     * 
     * @param context Application context
     * @param intervalSeconds Location update interval in seconds (e.g., 10, 30, 60)
     * @return true if tracking started successfully, false otherwise
     */
    @JvmStatic
    fun startTracking(context: Context, intervalSeconds: Long): Boolean {
        return try {
            // Validate interval (minimum 5 seconds)
            val validInterval = maxOf(5L, intervalSeconds)
            
            // Check permissions
            if (!hasLocationPermissions(context)) {
                Log.w(LogTags.LOCATION, "Location permissions not granted")
                return false
            }

            // Check if service is already running
            if (isServiceRunning(context, LocationTrackingService::class.java)) {
                Log.d(LogTags.LOCATION, "Tracking service is already running")
                return true
            }

            // Create notification channel before starting service (required for foreground service)
            NotificationHelper.createChannel(context)
            
            // Ensure channel is ready
            if (!NotificationHelper.ensureChannelReady(context)) {
                Log.e(LogTags.LOCATION, "Notification channel is not ready. Cannot start foreground service.")
                return false
            }

            // Request battery optimization exemption if needed
            if (context is Activity &&
                !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
            }

            // Start foreground service with interval
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_INTERVAL_MS, TimeUnit.SECONDS.toMillis(validInterval))
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(LogTags.LOCATION, "Location tracking started successfully with interval: ${validInterval}s")
            true
        } catch (e: Exception) {
            Log.e(LogTags.LOCATION, "Failed to start tracking", e)
            false
        }
    }

    /**
     * Stop location tracking service.
     * This will stop all location tracking and logging.
     * 
     * @param context Application context
     * @return true if tracking stopped successfully, false otherwise
     */
    @JvmStatic
    fun stopTracking(context: Context): Boolean {
        return try {
            val intent = Intent(context, LocationTrackingService::class.java)
            val stopped = context.stopService(intent)
            
            if (stopped) {
                Log.d(LogTags.LOCATION, "Location tracking stopped successfully")
            } else {
                Log.d(LogTags.LOCATION, "Tracking service was not running")
            }
            
            stopped
        } catch (e: Exception) {
            Log.e(LogTags.LOCATION, "Failed to stop tracking", e)
            false
        }
    }

    /**
     * Check if location tracking is currently active.
     * 
     * @param context Application context
     * @return true if tracking service is running, false otherwise
     */
    @JvmStatic
    fun isTracking(context: Context): Boolean {
        return isServiceRunning(context, LocationTrackingService::class.java)
    }

    /**
     * Check if required location permissions are granted.
     * 
     * @param context Application context
     * @return true if permissions are granted, false otherwise
     */
    @JvmStatic
    fun hasLocationPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for Android < Q
        }

        return fineLocation && backgroundLocation
    }

    /**
     * Sync location data from database.
     * This method retrieves all location logs and provides them via callback.
     * 
     * @param context Application context
     * @param time Time interval in seconds (used for filtering or processing)
     * @param callback Callback to receive the synced location data
     */
    @JvmStatic
    fun syncData(context: Context, time: Long, callback: SyncCallback) {
        sdkScope.launch {
            try {
                val database = AppDatabase.create(context)
                val repository = LocationRepository(database.locationDao())
                
                val locations = repository.getAll()
                
                if (locations.isNotEmpty()) {
                    Log.d(LogTags.LOCATION, "Syncing ${locations.size} location records with time interval: ${time}s")
                    callback.onSyncData(locations, time)
                } else {
                    Log.d(LogTags.LOCATION, "No location data to sync")
                    callback.onSyncError("No location data available")
                }
            } catch (e: Exception) {
                Log.e(LogTags.LOCATION, "Error syncing location data", e)
                callback.onSyncError(e.message ?: "Unknown error occurred")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        return services.any { it.service.className == serviceClass.name }
    }
}
