package com.example.sync

data class SyncStatus(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncTimeMillis: Long? = null,
    val lastError: String? = null
)
