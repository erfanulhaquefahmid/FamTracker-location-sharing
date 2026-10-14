package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FamilyEntity
import com.example.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: FamilyEntity)

    @Query("SELECT * FROM families WHERE familyId = :familyId LIMIT 1")
    fun getFamilyFlow(familyId: String): Flow<FamilyEntity?>

    @Query("SELECT * FROM families WHERE familyId = :familyId LIMIT 1")
    suspend fun getFamily(familyId: String): FamilyEntity?

    @Query("SELECT * FROM families WHERE inviteCode = :code LIMIT 1")
    suspend fun findFamilyByInviteCode(code: String): FamilyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<FamilyMemberEntity>)

    @Query("SELECT * FROM family_members WHERE familyId = :familyId ORDER BY displayName ASC")
    fun getMembersFlow(familyId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE familyId = :familyId ORDER BY displayName ASC")
    suspend fun getMembers(familyId: String): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE uid = :uid LIMIT 1")
    suspend fun getMember(uid: String): FamilyMemberEntity?

    @Query("UPDATE family_members SET isSharing = :isSharing, lastSeenAt = :timestamp WHERE uid = :uid")
    suspend fun updateMemberSharing(uid: String, isSharing: Boolean, timestamp: Long)

    @Query("""
        UPDATE family_members 
        SET latestLatitude = :lat, 
            latestLongitude = :lng, 
            latestAccuracyMeters = :acc, 
            latestCapturedAt = :capturedAt,
            lastSeenAt = :capturedAt
        WHERE uid = :uid AND (latestCapturedAt IS NULL OR :capturedAt >= latestCapturedAt)
    """)
    suspend fun updateMemberLatestLocationIfNewer(
        uid: String,
        lat: Double,
        lng: Double,
        acc: Float,
        capturedAt: Long
    ): Int

    @Query("DELETE FROM family_members WHERE uid = :uid")
    suspend fun deleteMember(uid: String): Int

    @Query("DELETE FROM families WHERE familyId = :familyId")
    suspend fun deleteFamily(familyId: String): Int
}
