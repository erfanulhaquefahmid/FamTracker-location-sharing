package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.LocationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocationEvent(event: LocationEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocationEvents(events: List<LocationEventEntity>): List<Long>

    @Query("SELECT * FROM location_events WHERE syncState = 'PENDING' ORDER BY capturedAt ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int = 100): List<LocationEventEntity>

    @Query("SELECT COUNT(*) FROM location_events WHERE syncState = 'PENDING'")
    fun countPendingEventsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM location_events WHERE syncState = 'PENDING'")
    suspend fun countPendingEvents(): Int

    @Query("SELECT * FROM location_events WHERE uid = :uid ORDER BY capturedAt DESC LIMIT 1")
    fun getLatestLocationFlow(uid: String): Flow<LocationEventEntity?>

    @Query("SELECT * FROM location_events WHERE uid = :uid ORDER BY capturedAt DESC LIMIT 1")
    suspend fun getLatestLocation(uid: String): LocationEventEntity?

    @Query("SELECT * FROM location_events WHERE uid = :uid ORDER BY capturedAt DESC LIMIT :limit OFFSET :offset")
    fun getHistoryFlow(uid: String, limit: Int = 50, offset: Int = 0): Flow<List<LocationEventEntity>>

    @Query("UPDATE location_events SET syncState = 'SYNCED', uploadedAt = :uploadedAt WHERE eventId = :eventId")
    suspend fun markEventSynced(eventId: String, uploadedAt: Long)

    @Query("UPDATE location_events SET syncState = 'SYNCED', uploadedAt = :uploadedAt WHERE eventId IN (:eventIds)")
    suspend fun markEventsSynced(eventIds: List<String>, uploadedAt: Long)

    @Query("UPDATE location_events SET syncState = 'FAILED' WHERE eventId = :eventId")
    suspend fun markEventFailed(eventId: String)

    /**
     * Purges only ALREADY-SYNCED records older than retention threshold.
     * Pending records are preserved unconditionally.
     */
    @Query("DELETE FROM location_events WHERE syncState = 'SYNCED' AND capturedAt < :retentionCutoff")
    suspend fun purgeSyncedRecordsOlderThan(retentionCutoff: Long): Int

    @Query("DELETE FROM location_events WHERE uid = :uid")
    suspend fun clearHistoryForUser(uid: String): Int

    @Query("SELECT COUNT(*) FROM location_events")
    suspend fun countTotalRecords(): Int
}
