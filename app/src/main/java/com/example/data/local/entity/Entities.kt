package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.LocationEvent
import com.example.model.MemberRole
import com.example.model.SyncState

@Entity(
    tableName = "location_events",
    indices = [
        Index(value = ["uid", "capturedAt"]),
        Index(value = ["syncState", "capturedAt"]),
        Index(value = ["familyId", "capturedAt"])
    ]
)
data class LocationEventEntity(
    @PrimaryKey
    val eventId: String,
    val uid: String,
    val familyId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAt: Long,
    val recordedAt: Long,
    val provider: String,
    val syncState: String, // PENDING, SYNCED, FAILED
    val uploadedAt: Long?
) {
    fun toDomain(): LocationEvent {
        return LocationEvent(
            eventId = eventId,
            uid = uid,
            familyId = familyId,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            capturedAt = capturedAt,
            recordedAt = recordedAt,
            provider = provider,
            syncState = SyncState.valueOf(syncState),
            uploadedAt = uploadedAt
        )
    }

    companion object {
        fun fromDomain(event: LocationEvent): LocationEventEntity {
            return LocationEventEntity(
                eventId = event.eventId,
                uid = event.uid,
                familyId = event.familyId,
                latitude = event.latitude,
                longitude = event.longitude,
                accuracyMeters = event.accuracyMeters,
                capturedAt = event.capturedAt,
                recordedAt = event.recordedAt,
                provider = event.provider,
                syncState = event.syncState.name,
                uploadedAt = event.uploadedAt
            )
        }
    }
}

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey
    val familyId: String,
    val name: String,
    val ownerUid: String,
    val inviteCode: String,
    val createdAt: Long
)

@Entity(
    tableName = "family_members",
    indices = [Index(value = ["familyId"])]
)
data class FamilyMemberEntity(
    @PrimaryKey
    val uid: String,
    val familyId: String,
    val displayName: String,
    val role: String, // OWNER, MEMBER
    val isSharing: Boolean,
    val lastSeenAt: Long?,
    val latestLatitude: Double?,
    val latestLongitude: Double?,
    val latestAccuracyMeters: Float?,
    val latestCapturedAt: Long?
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val uid: String,
    val displayName: String,
    val email: String?,
    val familyId: String?,
    val hasConsentedToTracking: Boolean,
    val collectionIntervalMs: Long,
    val retentionDays: Int
)
