package com.example.data.repository

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserProfileEntity
import com.example.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class UserRepository(
    private val userDao: UserDao
) {

    fun observeCurrentUser(): Flow<UserProfile?> {
        return userDao.getCurrentUserFlow().map { entity ->
            entity?.let {
                UserProfile(
                    uid = it.uid,
                    displayName = it.displayName,
                    email = it.email,
                    familyId = it.familyId,
                    hasConsentedToTracking = it.hasConsentedToTracking,
                    collectionIntervalMs = it.collectionIntervalMs,
                    retentionDays = it.retentionDays
                )
            }
        }
    }

    suspend fun getCurrentUser(): UserProfile? {
        val entity = userDao.getCurrentUser() ?: return null
        return UserProfile(
            uid = entity.uid,
            displayName = entity.displayName,
            email = entity.email,
            familyId = entity.familyId,
            hasConsentedToTracking = entity.hasConsentedToTracking,
            collectionIntervalMs = entity.collectionIntervalMs,
            retentionDays = entity.retentionDays
        )
    }

    suspend fun getOrCreateDefaultUser(displayName: String): UserProfile {
        val existing = getCurrentUser()
        if (existing != null) return existing

        val newUid = UUID.randomUUID().toString()
        val defaultProfile = UserProfile(
            uid = newUid,
            displayName = displayName.ifBlank { "Family Member" },
            email = null,
            familyId = null,
            hasConsentedToTracking = false,
            collectionIntervalMs = 120_000L,
            retentionDays = 30
        )

        userDao.insertOrUpdateProfile(
            UserProfileEntity(
                uid = defaultProfile.uid,
                displayName = defaultProfile.displayName,
                email = defaultProfile.email,
                familyId = defaultProfile.familyId,
                hasConsentedToTracking = defaultProfile.hasConsentedToTracking,
                collectionIntervalMs = defaultProfile.collectionIntervalMs,
                retentionDays = defaultProfile.retentionDays
            )
        )
        return defaultProfile
    }

    suspend fun updateConsent(uid: String, consented: Boolean) {
        userDao.updateConsent(uid, consented)
    }

    suspend fun setFamilyId(uid: String, familyId: String?) {
        userDao.updateFamilyId(uid, familyId)
    }

    suspend fun updateRetention(uid: String, days: Int) {
        userDao.updateRetention(uid, days)
    }

    suspend fun clearUser() {
        userDao.clearCurrentUser()
    }
}
