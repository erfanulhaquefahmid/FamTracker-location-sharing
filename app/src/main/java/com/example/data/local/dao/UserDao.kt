package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getCurrentUser(): UserProfileEntity?

    @Query("UPDATE user_profile SET hasConsentedToTracking = :consented WHERE uid = :uid")
    suspend fun updateConsent(uid: String, consented: Boolean)

    @Query("UPDATE user_profile SET familyId = :familyId WHERE uid = :uid")
    suspend fun updateFamilyId(uid: String, familyId: String?)

    @Query("UPDATE user_profile SET retentionDays = :days WHERE uid = :uid")
    suspend fun updateRetention(uid: String, days: Int)

    @Query("DELETE FROM user_profile")
    suspend fun clearCurrentUser()
}
