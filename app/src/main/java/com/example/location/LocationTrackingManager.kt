package com.example.location

import android.content.Context
import com.example.model.TrackingState
import kotlinx.coroutines.flow.StateFlow

class LocationTrackingManager(private val context: Context) {

    val trackingState: StateFlow<TrackingState> = LocationForegroundService.serviceState

    fun startTracking(uid: String, familyId: String) {
        LocationForegroundService.start(context, uid, familyId)
    }

    fun stopTracking() {
        LocationForegroundService.stop(context)
    }
}
