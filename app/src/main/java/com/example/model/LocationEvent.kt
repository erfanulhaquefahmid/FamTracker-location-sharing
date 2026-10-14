package com.example.model

data class LocationEvent(
    val eventId: String,
    val uid: String,
    val familyId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAt: Long,
    val recordedAt: Long,
    val provider: String = "fused",
    val syncState: SyncState = SyncState.PENDING,
    val uploadedAt: Long? = null
) {
    init {
        require(isValidCoordinate(latitude, longitude)) {
            "Invalid coordinates: lat=$latitude, lng=$longitude"
        }
        require(isValidAccuracy(accuracyMeters)) {
            "Invalid accuracy: $accuracyMeters meters"
        }
        require(capturedAt > 0) {
            "Invalid capture timestamp: $capturedAt"
        }
    }

    companion object {
        fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
            return latitude in -90.0..90.0 && longitude in -180.0..180.0 && !latitude.isNaN() && !longitude.isNaN()
        }

        fun isValidAccuracy(accuracyMeters: Float): Boolean {
            return accuracyMeters >= 0f && accuracyMeters < 5000f && !accuracyMeters.isNaN()
        }
    }
}
