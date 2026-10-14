package com.example.model

data class Family(
    val familyId: String,
    val name: String,
    val ownerUid: String,
    val inviteCode: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MemberRole {
    OWNER,
    MEMBER
}

data class FamilyMember(
    val uid: String,
    val familyId: String,
    val displayName: String,
    val role: MemberRole = MemberRole.MEMBER,
    val isSharing: Boolean = false,
    val lastSeenAt: Long? = null,
    val latestLatitude: Double? = null,
    val latestLongitude: Double? = null,
    val latestAccuracyMeters: Float? = null,
    val latestCapturedAt: Long? = null
)

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String? = null,
    val familyId: String? = null,
    val hasConsentedToTracking: Boolean = false,
    val collectionIntervalMs: Long = 120_000L, // 120 seconds target
    val retentionDays: Int = 30
)
