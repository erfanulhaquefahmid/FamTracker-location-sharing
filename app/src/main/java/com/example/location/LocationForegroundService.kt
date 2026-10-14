package com.example.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.repository.FamilyRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.UserRepository
import com.example.model.TrackingState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service that utilizes [FusedLocationProviderClient] to continuously collect
 * high-accuracy GPS coordinates in the background (120s interval) and durably persist
 * each event into the local Room database.
 */
class LocationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRepository: LocationRepository
    private lateinit var familyRepository: FamilyRepository
    private lateinit var userRepository: UserRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private var activeUid: String = ""
    private var activeFamilyId: String = ""

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "LocationService"
        const val PREFS_NAME = "famtrack_tracking_service_prefs"
        const val KEY_IS_TRACKING = "is_tracking"
        const val KEY_UID = "active_uid"
        const val KEY_FAMILY_ID = "active_family_id"

        const val CHANNEL_ID = "famtrack_active_tracking_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.example.famtrack.START_TRACKING"
        const val ACTION_STOP = "com.example.famtrack.STOP_TRACKING"
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_FAMILY_ID = "extra_family_id"

        // Desired tracking cadence: 120 seconds target
        const val TARGET_INTERVAL_MS = 120_000L
        const val FASTEST_INTERVAL_MS = 60_000L

        private val _serviceState = MutableStateFlow(TrackingState.STOPPED)
        val serviceState: StateFlow<TrackingState> = _serviceState.asStateFlow()

        fun start(context: Context, uid: String, familyId: String) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_UID, uid)
                putExtra(EXTRA_FAMILY_ID, familyId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(applicationContext)
        locationRepository = LocationRepository(db.locationDao(), db.familyDao())
        familyRepository = FamilyRepository(db.familyDao())
        userRepository = UserRepository(db.userDao())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FamTrack:LocationPersistenceLock")

        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (activeUid.isBlank() || activeFamilyId.isBlank()) {
                    Log.w(TAG, "Location received but activeUid or activeFamilyId is blank")
                    return
                }

                // Acquire brief partial wake lock to guarantee local Room persistence completes
                wakeLock?.acquire(3000L)
                serviceScope.launch {
                    try {
                        val capturedAt = if (location.time > 0) location.time else System.currentTimeMillis()
                        val event = locationRepository.recordLocation(
                            uid = activeUid,
                            familyId = activeFamilyId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            capturedAt = capturedAt,
                            provider = location.provider ?: "fused"
                        )
                        Log.d(TAG, "Recorded location event ${event.eventId} to local DB (lat: ${event.latitude}, lng: ${event.longitude}, acc: ±${event.accuracyMeters}m)")
                        updateNotification(
                            "Location updated: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)} (±${location.accuracy.toInt()}m)"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to record location to database", e)
                    } finally {
                        if (wakeLock?.isHeld == true) {
                            try {
                                wakeLock?.release()
                            } catch (ignored: Exception) {}
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val uid = intent.getStringExtra(EXTRA_UID) ?: ""
                val familyId = intent.getStringExtra(EXTRA_FAMILY_ID) ?: ""
                if (uid.isNotBlank() && familyId.isNotBlank()) {
                    activeUid = uid
                    activeFamilyId = familyId
                    prefs.edit()
                        .putBoolean(KEY_IS_TRACKING, true)
                        .putString(KEY_UID, uid)
                        .putString(KEY_FAMILY_ID, familyId)
                        .apply()
                    startTrackingForeground()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopTrackingService()
            }
            else -> {
                // System restarted the service (START_STICKY)
                val wasTracking = prefs.getBoolean(KEY_IS_TRACKING, false)
                val savedUid = prefs.getString(KEY_UID, "") ?: ""
                val savedFamilyId = prefs.getString(KEY_FAMILY_ID, "") ?: ""

                if (wasTracking && savedUid.isNotBlank() && savedFamilyId.isNotBlank()) {
                    activeUid = savedUid
                    activeFamilyId = savedFamilyId
                    startTrackingForeground()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTrackingForeground() {
        _serviceState.value = TrackingState.STARTING
        val initialNotification = buildNotification("Initializing high-accuracy location tracking...")

        // Android 14+ (API 34) requires explicit FOREGROUND_SERVICE_TYPE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TARGET_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMaxUpdateDelayMillis(TARGET_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            _serviceState.value = TrackingState.ACTIVE
            serviceScope.launch {
                familyRepository.updateMemberSharing(activeUid, true)
            }
            updateNotification("Sharing location with your family every 2 minutes")
            Log.i(TAG, "FusedLocationProviderClient tracking started successfully for UID: $activeUid")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates (missing permission)", e)
            _serviceState.value = TrackingState.WAITING_FOR_PERMISSION
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates from FusedLocationProviderClient", e)
            _serviceState.value = TrackingState.ERROR
            stopSelf()
        }
    }

    private fun stopTrackingService() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates", e)
        }

        prefs.edit().putBoolean(KEY_IS_TRACKING, false).apply()

        if (activeUid.isNotBlank()) {
            serviceScope.launch {
                familyRepository.updateMemberSharing(activeUid, false)
            }
        }

        _serviceState.value = TrackingState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "LocationForegroundService stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FamTrack Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies you when FamTrack is actively sharing your location."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FamTrack Location Sharing Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sharing", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (ignored: Exception) {}
        }
        _serviceState.value = TrackingState.STOPPED
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
