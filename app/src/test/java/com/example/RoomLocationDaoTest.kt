package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocationEventEntity
import com.example.model.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RoomLocationDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrievePendingEvents_preservesAscendingOrder() = runBlocking {
        val dao = database.locationDao()

        // Insert events with out-of-order capture timestamps
        val event1 = createEntity(capturedAt = 2000L, syncState = SyncState.PENDING)
        val event2 = createEntity(capturedAt = 1000L, syncState = SyncState.PENDING)
        val event3 = createEntity(capturedAt = 3000L, syncState = SyncState.PENDING)

        dao.insertLocationEvent(event1)
        dao.insertLocationEvent(event2)
        dao.insertLocationEvent(event3)

        assertEquals(3, dao.countPendingEvents())

        val pending = dao.getPendingEvents(limit = 10)
        assertEquals(3, pending.size)
        // Must be in ascending capturedAt order: 1000L, 2000L, 3000L
        assertEquals(1000L, pending[0].capturedAt)
        assertEquals(2000L, pending[1].capturedAt)
        assertEquals(3000L, pending[2].capturedAt)
    }

    @Test
    fun markEventSynced_updatesSyncState() = runBlocking {
        val dao = database.locationDao()
        val event = createEntity(capturedAt = 1000L, syncState = SyncState.PENDING)

        dao.insertLocationEvent(event)
        assertEquals(1, dao.countPendingEvents())

        dao.markEventSynced(event.eventId, uploadedAt = 5000L)
        assertEquals(0, dao.countPendingEvents())

        val history = dao.getLatestLocation(event.uid)
        assertEquals("SYNCED", history?.syncState)
        assertEquals(5000L, history?.uploadedAt)
    }

    @Test
    fun purgeSyncedRecords_neverDeletesPendingRecords() = runBlocking {
        val dao = database.locationDao()

        val oldSynced = createEntity(capturedAt = 100L, syncState = SyncState.SYNCED)
        val oldPending = createEntity(capturedAt = 100L, syncState = SyncState.PENDING)
        val recentSynced = createEntity(capturedAt = 500L, syncState = SyncState.SYNCED)

        dao.insertLocationEvent(oldSynced)
        dao.insertLocationEvent(oldPending)
        dao.insertLocationEvent(recentSynced)

        // Purge records older than 200L
        val purgedCount = dao.purgeSyncedRecordsOlderThan(retentionCutoff = 200L)
        assertEquals(1, purgedCount) // Only oldSynced should be deleted

        // Pending record must be preserved!
        assertEquals(1, dao.countPendingEvents())
        val pending = dao.getPendingEvents(limit = 10)
        assertEquals(oldPending.eventId, pending[0].eventId)
    }

    private fun createEntity(
        capturedAt: Long,
        syncState: SyncState
    ): LocationEventEntity {
        return LocationEventEntity(
            eventId = UUID.randomUUID().toString(),
            uid = "user_test_1",
            familyId = "family_test_1",
            latitude = 37.7749,
            longitude = -122.4194,
            accuracyMeters = 8.0f,
            capturedAt = capturedAt,
            recordedAt = capturedAt + 10L,
            provider = "fused",
            syncState = syncState.name,
            uploadedAt = if (syncState == SyncState.SYNCED) capturedAt + 50L else null
        )
    }
}
