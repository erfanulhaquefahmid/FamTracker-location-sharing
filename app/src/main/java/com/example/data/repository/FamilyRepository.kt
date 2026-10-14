package com.example.data.repository

import com.example.data.local.dao.FamilyDao
import com.example.data.local.entity.FamilyEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.model.Family
import com.example.model.FamilyMember
import com.example.model.MemberRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FamilyRepository(
    private val familyDao: FamilyDao
) {

    suspend fun createFamily(name: String, ownerUid: String, ownerDisplayName: String): Family {
        val familyId = UUID.randomUUID().toString()
        val inviteCode = generateInviteCode()
        val now = System.currentTimeMillis()

        val familyEntity = FamilyEntity(
            familyId = familyId,
            name = name.trim(),
            ownerUid = ownerUid,
            inviteCode = inviteCode,
            createdAt = now
        )
        familyDao.insertFamily(familyEntity)

        // Insert owner as first member
        val ownerMember = FamilyMemberEntity(
            uid = ownerUid,
            familyId = familyId,
            displayName = ownerDisplayName,
            role = MemberRole.OWNER.name,
            isSharing = false,
            lastSeenAt = now,
            latestLatitude = null,
            latestLongitude = null,
            latestAccuracyMeters = null,
            latestCapturedAt = null
        )
        familyDao.insertMember(ownerMember)

        return Family(familyId, name, ownerUid, inviteCode, now)
    }

    suspend fun joinFamilyByInviteCode(
        inviteCode: String,
        uid: String,
        displayName: String
    ): Result<Family> {
        val cleanCode = inviteCode.trim().uppercase()
        val familyEntity = familyDao.findFamilyByInviteCode(cleanCode)
            ?: return Result.failure(IllegalArgumentException("Invalid or expired invitation code: $cleanCode"))

        val member = FamilyMemberEntity(
            uid = uid,
            familyId = familyEntity.familyId,
            displayName = displayName,
            role = MemberRole.MEMBER.name,
            isSharing = false,
            lastSeenAt = System.currentTimeMillis(),
            latestLatitude = null,
            latestLongitude = null,
            latestAccuracyMeters = null,
            latestCapturedAt = null
        )
        familyDao.insertMember(member)

        return Result.success(
            Family(
                familyId = familyEntity.familyId,
                name = familyEntity.name,
                ownerUid = familyEntity.ownerUid,
                inviteCode = familyEntity.inviteCode,
                createdAt = familyEntity.createdAt
            )
        )
    }

    fun observeFamily(familyId: String): Flow<Family?> {
        return familyDao.getFamilyFlow(familyId).map { entity ->
            entity?.let {
                Family(it.familyId, it.name, it.ownerUid, it.inviteCode, it.createdAt)
            }
        }
    }

    fun observeMembers(familyId: String): Flow<List<FamilyMember>> {
        return familyDao.getMembersFlow(familyId).map { list ->
            list.map {
                FamilyMember(
                    uid = it.uid,
                    familyId = it.familyId,
                    displayName = it.displayName,
                    role = MemberRole.valueOf(it.role),
                    isSharing = it.isSharing,
                    lastSeenAt = it.lastSeenAt,
                    latestLatitude = it.latestLatitude,
                    latestLongitude = it.latestLongitude,
                    latestAccuracyMeters = it.latestAccuracyMeters,
                    latestCapturedAt = it.latestCapturedAt
                )
            }
        }
    }

    suspend fun updateMemberSharing(uid: String, isSharing: Boolean) {
        familyDao.updateMemberSharing(uid, isSharing, System.currentTimeMillis())
    }

    suspend fun removeMember(uid: String): Int {
        return familyDao.deleteMember(uid)
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
