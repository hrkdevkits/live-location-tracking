package com.hrk.tracking.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hrk.tracking.util.LogTags

/**
 * Created by Ritik on: 10/01/26
 * 
 * Boot receiver - can be used to restart tracking on device boot if needed
 */

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(LogTags.LOCATION, "Boot completed - tracking will start when user calls startTracking()")
            // Tracking will start when user explicitly calls LiveTrackingSDK.startTracking()
        }
    }
}