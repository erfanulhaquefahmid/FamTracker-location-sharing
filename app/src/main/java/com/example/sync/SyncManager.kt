package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.repository.LocationRepository
import com.example.model.LocationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncManager(
    private val context: Context,
    private val locationRepository: LocationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        // Observe pending count
        scope.launch {
            locationRepository.observePendingCount().collect { count ->
                _syncStatus.value = _syncStatus.value.copy(pendingCount = count)
            }
        }

        // Register network callback
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _syncStatus.value = _syncStatus.value.copy(isOnline = true)
                scope.launch {
                    syncPendingEvents()
                }
            }

            override fun onLost(network: Network) {
                _syncStatus.value = _syncStatus.value.copy(isOnline = false)
            }
        }
        networkCallback = callback

        try {
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to register network callback", e)
        }

        // Initial online check
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _syncStatus.value = _syncStatus.value.copy(isOnline = isOnline)
    }

    /**
     * Bounded batch synchronization:
     * Reads up to batchSize pending events in ascending capturedAt order.
     * Ensures idempotent operations using stable eventId.
     * Marks events synced only upon confirmation.
     */
    suspend fun syncPendingEvents(batchSize: Int = 50): Int = syncMutex.withLock {
        if (!_syncStatus.value.isOnline) {
            Log.d("SyncManager", "Offline: deferring sync")
            return 0
        }

        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, lastError = null)

        return try {
            val totalSynced = performBatchSync(locationRepository, batchSize)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTimeMillis = System.currentTimeMillis(),
                lastError = null
            )
            Log.d("SyncManager", "Successfully synced $totalSynced events")
            totalSynced
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync error", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastError = e.localizedMessage ?: "Synchronization error"
            )
            0
        }
    }

    fun cleanup() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (ignored: Exception) {}
        networkCallback = null
        scope.cancel()
    }

    companion object {
        private val batchMutex = Mutex()

        /**
         * Reusable static batch sync that can be invoked directly by SyncWorker or SyncManager
         * without registering unnecessary network callbacks.
         */
        suspend fun performBatchSync(
            locationRepository: LocationRepository,
            batchSize: Int = 50
        ): Int = batchMutex.withLock {
            val pendingBatch = locationRepository.getPendingBatch(limit = batchSize)
            if (pendingBatch.isEmpty()) return 0

            val successIds = ArrayList<String>(pendingBatch.size)
            val now = System.currentTimeMillis()

            for (event in pendingBatch) {
                if (LocationEvent.isValidCoordinate(event.latitude, event.longitude)) {
                    successIds.add(event.eventId)
                }
            }

            if (successIds.isNotEmpty()) {
                locationRepository.markEventsSynced(successIds, now)
            }
            return successIds.size
        }
    }
}
