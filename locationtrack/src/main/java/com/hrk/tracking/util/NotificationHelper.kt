package com.hrk.tracking.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log


/**
 * Created by Ritik on: 10/01/26
 */

object NotificationHelper {
    const val CHANNEL_ID = "tracking_channel"
    const val CHANNEL_NAME = "Live Tracking"

    /**
     * Creates the notification channel for foreground service.
     * This must be called before starting the foreground service.
     * Safe to call multiple times - will not recreate if channel already exists.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            
            // Check if channel already exists
            val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null) {
                Log.d(LogTags.LOCATION, "Notification channel already exists")
                // Ensure channel is not disabled
                if (existingChannel.importance == NotificationManager.IMPORTANCE_NONE) {
                    Log.w(LogTags.LOCATION, "Notification channel is disabled, recreating...")
                    manager.deleteNotificationChannel(CHANNEL_ID)
                } else {
                    return
                }
            }
            
            // Create channel with IMPORTANCE_LOW for foreground service
            // IMPORTANCE_LOW is the minimum required for foreground services
            val channel = NotificationChannel(
                CHANNEL_ID, 
                CHANNEL_NAME, 
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking in background"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            manager.createNotificationChannel(channel)
            Log.d(LogTags.LOCATION, "Notification channel created successfully")
        }
    }
    
    /**
     * Ensures the notification channel exists and is enabled.
     * Returns true if channel is ready, false otherwise.
     */
    fun ensureChannelReady(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context)
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true // Pre-Oreo doesn't need channels
    }
}
