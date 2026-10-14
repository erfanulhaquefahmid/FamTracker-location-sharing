package com.example.model

enum class FreshnessLevel {
    FRESH,       // <= 3 minutes
    DELAYED,     // > 3 minutes and <= 5 minutes
    STALE,       // > 5 minutes
    UNAVAILABLE  // No valid location
}

data class LocationFreshness(
    val level: FreshnessLevel,
    val ageMillis: Long,
    val label: String
) {
    companion object {
        const val FRESH_THRESHOLD_MS = 180_000L   // 3 minutes
        const val DELAYED_THRESHOLD_MS = 300_000L // 5 minutes

        fun calculate(capturedAt: Long?, currentTimeMillis: Long = System.currentTimeMillis()): LocationFreshness {
            if (capturedAt == null || capturedAt <= 0L) {
                return LocationFreshness(
                    level = FreshnessLevel.UNAVAILABLE,
                    ageMillis = -1L,
                    label = "Location unavailable"
                )
            }

            val ageMs = (currentTimeMillis - capturedAt).coerceAtLeast(0L)

            return when {
                ageMs <= FRESH_THRESHOLD_MS -> {
                    val label = if (ageMs < 60_000L) "Updated just now" else "Updated ${ageMs / 60_000L}m ago"
                    LocationFreshness(FreshnessLevel.FRESH, ageMs, label)
                }
                ageMs <= DELAYED_THRESHOLD_MS -> {
                    val label = "Delayed (${ageMs / 60_000L}m ago)"
                    LocationFreshness(FreshnessLevel.DELAYED, ageMs, label)
                }
                else -> {
                    val minutes = ageMs / 60_000L
                    val label = if (minutes < 60) {
                        "Stale location (${minutes}m ago)"
                    } else {
                        val hours = minutes / 60
                        "Stale location (${hours}h ago)"
                    }
                    LocationFreshness(FreshnessLevel.STALE, ageMs, label)
                }
            }
        }
    }
}
