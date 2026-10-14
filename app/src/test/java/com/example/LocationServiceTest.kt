package com.example

import com.example.location.LocationForegroundService
import com.example.model.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocationServiceTest {

    @Test
    fun serviceConstants_areCorrectlyConfigured() {
        assertEquals("famtrack_active_tracking_channel", LocationForegroundService.CHANNEL_ID)
        assertEquals(4040, LocationForegroundService.NOTIFICATION_ID)
        assertEquals("com.example.famtrack.START_TRACKING", LocationForegroundService.ACTION_START)
        assertEquals("com.example.famtrack.STOP_TRACKING", LocationForegroundService.ACTION_STOP)
        assertEquals(120_000L, LocationForegroundService.TARGET_INTERVAL_MS)
        assertEquals(60_000L, LocationForegroundService.FASTEST_INTERVAL_MS)
    }

    @Test
    fun initialServiceState_isStopped() {
        assertNotNull(LocationForegroundService.serviceState)
        assertEquals(TrackingState.STOPPED, LocationForegroundService.serviceState.value)
    }
}
