# FamTrack

**FamTrack** is a native Android application engineered for consensual, privacy-first family location sharing.

---

## Key Capabilities

1. **Explicit Consent & Foreground Tracking**
   - Consent must be granted before any tracking begins.
   - Ongoing notification keeps tracking state transparent at all times.
   - Instant "Stop Sharing" action stops the service immediately.

2. **Durable Local Storage (Room DB)**
   - Every valid captured location is saved to Room before any network attempt.
   - In offline scenarios, events are queued with their original capture timestamps.
   - Pending records are never purged by retention policies.

3. **High-Accuracy Fused Location**
   - Google Play Services Fused Location Provider.
   - 120-second target interval with priority `PRIORITY_HIGH_ACCURACY`.
   - Captures and displays horizontal accuracy in meters.

4. **Interactive Geographic Map**
   - OpenStreetMap (Osmdroid) renders family member positions.
   - Tap any member to view coordinates, capture time, and freshness chip.

5. **Location Freshness Model**
   - **Fresh:** within 3 minutes.
   - **Delayed:** 3 to 5 minutes.
   - **Stale:** older than 5 minutes.
   - **Unavailable:** no location recorded.

---

## Architecture
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Local DB:** Room Database (`AppDatabase`, `LocationDao`, `FamilyDao`, `UserDao`)
- **Location:** `LocationForegroundService` + `FusedLocationProviderClient`
- **Background Sync:** `SyncManager` + `WorkManager` (`SyncWorker`)
- **Backend Rules:** Hardened zero-trust `firestore.rules` ready for Cloud Firestore deployment.
