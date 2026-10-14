package com.example.data.repository

import com.example.data.local.dao.FamilyDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.entity.LocationEventEntity
import com.example.model.LocationEvent
import com.example.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocationRepository(
    private val locationDao: LocationDao,
    private val familyDao: FamilyDao
) {

    suspend fun recordLocation(
        uid: String,
        familyId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        capturedAt: Long,
        provider: String = "fused"
    ): LocationEvent {
        val eventId = UUID.randomUUID().toString()
        val recordedAt = System.currentTimeMillis()

        val event = LocationEvent(
            eventId = eventId,
            uid = uid,
            familyId = familyId,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            capturedAt = capturedAt,
            recordedAt = recordedAt,
            provider = provider,
            syncState = SyncState.PENDING,
            uploadedAt = null
        )

        // 1. Durable persistence to Room
        locationDao.insertLocationEvent(LocationEventEntity.fromDomain(event))

        // 2. Monotonic update of latest member location (only if capturedAt is newer)
        familyDao.updateMemberLatestLocationIfNewer(
            uid = uid,
            lat = latitude,
            lng = longitude,
            acc = accuracyMeters,
            capturedAt = capturedAt
        )

        return event
    }

    fun observePendingCount(): Flow<Int> = locationDao.countPendingEventsFlow()

    fun observeLatestLocation(uid: String): Flow<LocationEvent?> =
        locationDao.getLatestLocationFlow(uid).map { it?.toDomain() }

    fun observeHistory(uid: String, limit: Int = 100, offset: Int = 0): Flow<List<LocationEvent>> =
        locationDao.getHistoryFlow(uid, limit, offset).map { list -> list.map { it.toDomain() } }

    suspend fun getPendingBatch(limit: Int = 50): List<LocationEvent> {
        return locationDao.getPendingEvents(limit).map { it.toDomain() }
    }

    suspend fun markEventsSynced(eventIds: List<String>, uploadedAt: Long) {
        locationDao.markEventsSynced(eventIds, uploadedAt)
    }

    suspend fun purgeSyncedRecordsOlderThan(retentionDays: Int): Int {
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000L)
        return locationDao.purgeSyncedRecordsOlderThan(cutoff)
    }

    suspend fun clearHistoryForUser(uid: String) {
        locationDao.clearHistoryForUser(uid)
    }
}
