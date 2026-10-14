# FamTrack — Setup & Build Guide

## 1. Project Overview
FamTrack is a native Android application built using Kotlin and Jetpack Compose for consensual family location sharing. It features:
- **High-Accuracy Location Tracking:** 120-second target sampling interval using Google Play Services Fused Location Provider.
- **Durable Offline Storage:** Room Database for persistent local queue of all captured locations.
- **Offline-First Synchronization:** WorkManager and Network-aware synchronization preserving capture order and protecting monotonic latest location.
- **Interactive Geographic Map:** OpenStreetMap (Osmdroid) visualization of family members with reported accuracy circles and freshness badges.
- **Privacy & Consent First:** Explicit user consent required before tracking; ongoing foreground service notification with instant "Stop Sharing" action.

---

## 2. Prerequisites
- **JDK:** OpenJDK 17 or higher
- **Android Studio:** Ladybug (2024.2.1) or newer
- **Android SDK:** Compile SDK 36, Min SDK 24
- **Google Play Services:** Available on device or emulator

---

## 3. Opening in Android Studio
1. Open Android Studio.
2. Select **File > Open...** and navigate to this project's root folder.
3. Allow Gradle to synchronize dependencies.
4. Run the app on an Android device or emulator with Google APIs.

---

## 4. Building the APK
To assemble the debug APK from the terminal:
```bash
gradle assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To install on a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Running Automated Tests
Run all unit and Robolectric database tests:
```bash
gradle :app:testDebugUnitTest
```
Tests included:
- `LocationValidationTest`: Validates latitude/longitude bounds, NaN handling, and accuracy limits.
- `LocationFreshnessTest`: Tests exact 3-minute and 5-minute freshness thresholds and formatting.
- `MonotonicLatestTest`: Verifies out-of-order offline queue events do not roll back newer latest-location states.
- `RoomLocationDaoTest`: In-memory Robolectric tests verifying ascending queue order, sync state transitions, and retention policy.

---

## 6. Optional: Connecting Cloud Firestore
When you are ready to connect a live Firebase project:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create or select your Firebase project.
3. Add an Android app with package name: `com.aistudio.famtrack.ktxy`.
4. Register your debug SHA-1 fingerprint (from your debug keystore).
5. Download `google-services.json` and place it in the `app/` directory (`app/google-services.json`).
6. Deploy the included security rules:
   ```bash
   firebase deploy --only firestore:rules
   ```
   (The rules are pre-configured in `firestore.rules` at the root of this project).
