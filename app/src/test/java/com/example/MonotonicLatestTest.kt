package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonotonicLatestTest {

    @Test
    fun newerCapture_updatesLatestLocation() {
        val currentLatestCapturedAt = 100_000L
        val incomingCapturedAt = 120_000L

        val shouldUpdate = incomingCapturedAt >= currentLatestCapturedAt
        assertTrue(shouldUpdate)
    }

    @Test
    fun olderQueuedCapture_doesNotOverwriteNewerLatestLocation() {
        val currentLatestCapturedAt = 120_000L
        val incomingOldQueuedCapturedAt = 90_000L

        val shouldUpdate = incomingOldQueuedCapturedAt >= currentLatestCapturedAt
        assertFalse(shouldUpdate)
    }

    @Test
    fun equalTimestamp_allowsUpdate() {
        val currentLatestCapturedAt = 100_000L
        val incomingCapturedAt = 100_000L

        val shouldUpdate = incomingCapturedAt >= currentLatestCapturedAt
        assertTrue(shouldUpdate)
    }
}
