# Live Location Tracking SDK - Setup Guide

A lightweight Android SDK for tracking location in both foreground and background with customizable update intervals and optional data synchronization.

**Repository:** https://github.com/hrkdevkits/live-location-tracking

---

## Features

- **Simple SDK Interface**: Easy-to-use `LiveTrackingSDK` with minimal API surface
- **Foreground & Background Tracking**: Reliable tracking when app is closed
- **Customizable Interval**: Set tracking interval (10s, 30s, 60s, or any value ≥5s)
- **Optional Data Sync**: `syncData()` method to retrieve location logs
- **Offline Storage**: Room Database for local storage
- **Battery Optimization**: Automatic battery optimization exemption request
- **Zero External APIs**: No external API dependencies

---

## Installation

### Option 1: JitPack (Recommended)

**1. Add JitPack repository** in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2. Add dependency** in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.hrkdevkits:live-location-tracking:1.0.0")
}
```

**3. Sync Gradle**

### Option 2: Local Module

1. Clone the repository
2. Copy the `locationtrack` module into your project
3. In `settings.gradle.kts` add: `include(":locationtrack")`
4. In your app's `build.gradle.kts` add: `implementation(project(":locationtrack"))`

---

## Quick Start

### 1. Add Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

### 2. Register Service (AndroidManifest.xml)

Add inside your `<application>` tag:

```xml
<service
    android:name="com.hrk.tracking.service.LocationTrackingService"
    android:foregroundServiceType="location"
    android:exported="false"/>
```

### 3. Initialize SDK

```kotlin
import com.hrk.tracking.sdk.LiveTrackingSDK

// Start tracking (default 10 seconds)
LiveTrackingSDK.startTracking(context)

// Start tracking with custom interval (30 seconds)
LiveTrackingSDK.startTracking(context, 30)

// Stop tracking
LiveTrackingSDK.stopTracking(context)

// Check if tracking is active
val isTracking = LiveTrackingSDK.isTracking(context)
```

### 4. Request Permissions

```kotlin
if (!LiveTrackingSDK.hasLocationPermissions(context)) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ),
        PERMISSION_REQUEST_CODE
    )
}
```

---

## API Reference

| Method | Description |
|--------|-------------|
| `startTracking(context)` | Start with default 10s interval |
| `startTracking(context, intervalSeconds)` | Start with custom interval (min 5s) |
| `stopTracking(context)` | Stop tracking service |
| `isTracking(context)` | Check if tracking is active |
| `hasLocationPermissions(context)` | Check if permissions granted |
| `syncData(context, time, callback)` | Retrieve location data via callback |

### Sync Data Example

```kotlin
LiveTrackingSDK.syncData(this, time = 60, object : SyncCallback {
    override fun onSyncData(locations: List<LocationEntity>, timeInterval: Long) {
        locations.forEach { location ->
            println("Lat: ${location.latitude}, Lng: ${location.longitude}")
        }
    }
    override fun onSyncError(error: String?) {
        println("Sync error: $error")
    }
})
```

---

## Requirements

- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 36)
- **Kotlin**: 2.0+
- **Gradle**: 8.13+

---

## Pushing to GitHub

```bash
git remote add origin https://github.com/hrkdevkits/live-location-tracking.git
# Or update existing remote:
git remote set-url origin https://github.com/hrkdevkits/live-location-tracking.git

git add .
git commit -m "Initial commit: Live Location Tracking SDK"
git branch -M main
git push -u origin main
```

## Publishing to JitPack

1. Push code to GitHub
2. Create release tag: `git tag -a v1.0.0 -m "Release 1.0.0"`
3. Push tag: `git push origin v1.0.0`
4. JitPack auto-builds at: https://jitpack.io/#hrkdevkits/live-location-tracking

---

## Troubleshooting

**Location not captured?**
- Verify permissions granted
- Enable location services
- Grant battery optimization exemption

**Service stops in background?**
- Grant battery optimization exemption (SDK requests automatically)
- Ensure notification is showing (foreground service requirement)

**High battery usage?**
- Use longer intervals (30s, 60s instead of 10s)

**Build logs:** Filter with `adb logcat | grep LOCATION_TRACK`

---

## Project Structure

```
locationtrack/
├── sdk/          → LiveTrackingSDK, SyncCallback
├── service/      → LocationTrackingService
├── data/         → AppDatabase, LocationDao, LocationEntity
├── util/         → Config, NotificationHelper, BatteryOptimizationHelper
└── receiver/     → BootCompletedReceiver
```

---

## License

MIT License - see [LICENSE](LICENSE) file.
