package com.example

import com.example.model.FreshnessLevel
import com.example.model.LocationFreshness
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationFreshnessTest {

    private val now = 1_000_000_000L

    @Test
    fun nullOrZeroTimestamp_isUnavailable() {
        val freshnessNull = LocationFreshness.calculate(null, currentTimeMillis = now)
        assertEquals(FreshnessLevel.UNAVAILABLE, freshnessNull.level)

        val freshnessZero = LocationFreshness.calculate(0L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.UNAVAILABLE, freshnessZero.level)
    }

    @Test
    fun withinThreeMinutes_isFresh() {
        // Just now
        val fresh0 = LocationFreshness.calculate(now, currentTimeMillis = now)
        assertEquals(FreshnessLevel.FRESH, fresh0.level)
        assertEquals("Updated just now", fresh0.label)

        // 2 minutes ago
        val fresh2m = LocationFreshness.calculate(now - 120_000L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.FRESH, fresh2m.level)
        assertEquals("Updated 2m ago", fresh2m.label)

        // Boundary: exactly 3 minutes
        val fresh3m = LocationFreshness.calculate(now - 180_000L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.FRESH, fresh3m.level)
    }

    @Test
    fun betweenThreeAndFiveMinutes_isDelayed() {
        // Boundary: 3 minutes + 1 ms
        val delayedJustPast3m = LocationFreshness.calculate(now - 180_001L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.DELAYED, delayedJustPast3m.level)

        // 4 minutes ago
        val delayed4m = LocationFreshness.calculate(now - 240_000L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.DELAYED, delayed4m.level)

        // Boundary: exactly 5 minutes
        val delayed5m = LocationFreshness.calculate(now - 300_000L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.DELAYED, delayed5m.level)
    }

    @Test
    fun beyondFiveMinutes_isStale() {
        // Boundary: 5 minutes + 1 ms
        val staleJustPast5m = LocationFreshness.calculate(now - 300_001L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.STALE, staleJustPast5m.level)

        // 15 minutes ago
        val stale15m = LocationFreshness.calculate(now - 900_000L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.STALE, stale15m.level)

        // 2 hours ago
        val stale2h = LocationFreshness.calculate(now - 7_200_000L, currentTimeMillis = now)
        assertEquals(FreshnessLevel.STALE, stale2h.level)
    }
}
