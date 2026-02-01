package com.hrk.tracking.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hrk.tracking.data.repository.LocationRepository
import com.hrk.tracking.data.local.AppDatabase
import com.hrk.tracking.data.local.LocationEntity
import com.hrk.tracking.util.Config
import com.hrk.tracking.util.LogTags
import com.hrk.tracking.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Created by Ritik on: 10/01/26
 */

class LocationTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var repository: LocationRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var locationUpdateIntervalMs = Config.LOCATION_UPDATE_INTERVAL_MS

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()

        if (!hasLocationPermission()) {
            Log.e(LogTags.LOCATION, "Location permission not granted, stopping service")
            stopSelf()
            return
        }

        // Ensure notification channel exists before creating notification
        NotificationHelper.createChannel(this)
        
        if (!NotificationHelper.ensureChannelReady(this)) {
            Log.e(LogTags.LOCATION, "Notification channel is not ready, stopping service")
            stopSelf()
            return
        }

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        repository = LocationRepository(
            AppDatabase.create(this).locationDao()
        )

        try {
            val notification = createNotification()
            startForeground(1, notification)
            Log.d(LogTags.LOCATION, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(LogTags.LOCATION, "Failed to start foreground service", e)
            e.printStackTrace()
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get custom interval from intent if provided
        val intervalFromIntent = intent?.getLongExtra("interval_ms", Config.LOCATION_UPDATE_INTERVAL_MS) 
            ?: Config.LOCATION_UPDATE_INTERVAL_MS
        
        if (intervalFromIntent != locationUpdateIntervalMs) {
            locationUpdateIntervalMs = intervalFromIntent
            // Restart location updates with new interval
            if (::fusedClient.isInitialized) {
                fusedClient.removeLocationUpdates(locationCallback)
                requestLocationUpdates()
            }
        } else {
            // Start location updates if not already started
            if (::fusedClient.isInitialized) {
                requestLocationUpdates()
            }
        }
        
        return START_STICKY
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, locationUpdateIntervalMs
        ).build()

        Log.d(LogTags.LOCATION, "Requesting location updates with interval: ${locationUpdateIntervalMs}ms")
        fusedClient.requestLocationUpdates(
            request, locationCallback, Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: run {
                Log.w(LogTags.LOCATION, "Location result is null")
                return
            }

            Log.d(
                LogTags.LOCATION,
                "Location received → lat=${location.latitude}, " + "lng=${location.longitude}, acc=${location.accuracy}, " + "speed=${location.speed}"
            )

            serviceScope.launch {
                val saved = repository.save(
                    LocationEntity(
                        employeeId = "EMP001",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        speed = if (location.hasSpeed()) location.speed else null,
                        timestamp = System.currentTimeMillis()
                    )
                )
                if (!saved) {
                    Log.e(LogTags.LOCATION, "Failed to save location to database")
                }
            }
        }
    }


    private fun createNotification(): Notification {
        // For library modules, we need to find the icon resource
        // Try library package name first, then app package name
        val iconRes = getNotificationIcon()
        
        if (iconRes == 0) {
            Log.e(LogTags.LOCATION, "Failed to find notification icon, cannot create notification")
            throw IllegalStateException("Notification icon resource not found")
        }
        
        return NotificationCompat.Builder(
            this, NotificationHelper.CHANNEL_ID
        )
            .setContentTitle("Live Tracking Active 🟢")
            .setContentText("Location tracking running..")
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun getNotificationIcon(): Int {
        // Try library package name first (com.hrk.tracking)
        var resId = try {
            resources.getIdentifier(
                "ic_location_notification",
                "drawable",
                "com.hrk.tracking"
            )
        } catch (e: Exception) {
            Log.w(LogTags.LOCATION, "Error getting icon with library package", e)
            0
        }
        
        if (resId != 0) {
            Log.d(LogTags.LOCATION, "Found notification icon using library package name: $resId")
            return resId
        }
        
        // Try app package name (where resources are merged)
        resId = try {
            resources.getIdentifier(
                "ic_location_notification",
                "drawable",
                packageName
            )
        } catch (e: Exception) {
            Log.w(LogTags.LOCATION, "Error getting icon with app package", e)
            0
        }
        
        if (resId != 0) {
            Log.d(LogTags.LOCATION, "Found notification icon using app package name: $resId")
            return resId
        }
        
        // Try without package name (resources merged into app)
        resId = try {
            resources.getIdentifier(
                "ic_location_notification",
                "drawable",
                null
            )
        } catch (e: Exception) {
            Log.w(LogTags.LOCATION, "Error getting icon without package", e)
            0
        }
        
        if (resId != 0) {
            Log.d(LogTags.LOCATION, "Found notification icon without package name: $resId")
            return resId
        }
        
        // Fallback: use a valid system notification icon
        // ic_stat_notify is a valid notification icon
        Log.w(LogTags.LOCATION, "Using fallback system icon for notification")
        return android.R.drawable.ic_dialog_info
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedClient.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


