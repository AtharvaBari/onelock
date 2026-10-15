# OneLock 🔒

A flagship, production-grade Android App Lock application built with Kotlin and Jetpack Compose, tailored with native **Samsung One UI** design aesthetics, smooth 120Hz performance, and defense-in-depth system security engines.

![OneLock Banner](https://img.shields.io/badge/Samsung-One%20UI%20Dark%20Mode-000000?style=for-the-badge&logo=samsung&logoColor=387AFF)
![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.05-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

---

## ✨ Features

### 1. Samsung One UI Architecture & Aesthetics
- **One UI 4-Tab Navigation**: Persistent bottom navigation bar (`#0B0B0D` surface, `#387AFF` indicator) dividing duties across:
  - **Home**: Live protection status badge, 2x2 Bento Summary Grid (Locked Apps, Total Apps, Masked Notifications, Intruder Alerts), categorized filter chips (`All`, `Locked`, `Social & Messaging`, `System & Settings`, `Custom Rules`), search bar, and app list.
  - **Vault & Logs**: Front-camera intruder selfie gallery with app & timestamp metadata, plus private intercepted notification feed.
  - **Protection Shield**: Grouped One UI switches for Anti-Tamper, Privacy Shield, Notification Masking, and Intruder Trap.
  - **Settings**: Master credential setup (PIN 4/6, Pattern, Password), scrambled keypad toggle, hidden pattern trail, smart re-lock timeouts, and permission health check.
- **Samsung One UI Keyguard Lock Screen**:
  - AMOLED `#000000` / deep frosted `#F508080A` backdrop with squircle app icons.
  - Interactive PIN dots with horizontal shake animations and waveform haptic feedback (`VibrationEffect.createWaveform`).
  - Frosted circular keypad buttons with sub-labels (`ABC`, `DEF`), number scrambling, and biometric trigger.
  - Custom 3x3 interactive Pattern lock canvas with optional hidden trails.

### 2. Five Core Security Engines
1. **Interactive Multi-Window & Pop-up View Session Engine**:
   - Supports Samsung Multi-Window, Split-Screen, and Pop-up floating windows via `FLAG_RETRIEVE_INTERACTIVE_WINDOWS`.
   - Tracks concurrent visible applications (`visibleAppPackages`) so switching focus between floating windows does not trigger re-lock loops.
   - Built-in 800ms grace debounce preventing animation or resize race conditions.
2. **Anti-Tamper & Force-Stop Protection**:
   - Real-time accessibility hierarchy inspection inside `com.android.settings`, `com.google.android.packageinstaller`, and Samsung uninstaller dialogs.
   - Blocks unauthorized attempts to Force Stop, Uninstall, Clear Data, or disable Accessibility privileges by launching self-protection.
3. **Multitasking Privacy Shield**:
   - `WindowManager.LayoutParams.FLAG_SECURE` blocks screenshots and screen recordings on all sensitive activities.
   - Frosted privacy overlay masks app contents when entering Recents / Overview multitasking switcher.
4. **Notification Content Masking**:
   - Native `NotificationListenerService` intercepts incoming notifications from locked apps.
   - Replaces notifications with a masked system alert (*"🔒 Locked notification — Tap to view in OneLock"*) while logging message previews securely inside the local Vault.
5. **CameraX Silent Intruder Capture**:
   - Front camera trap automatically triggers on 2 consecutive wrong credential attempts.
   - Captures and stores timestamped evidence securely in private app storage.

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose (BOM `2024.05.00`) with Material 3 & Material Icons Extended
- **Camera**: AndroidX CameraX 1.3.3 (`core`, `camera2`, `lifecycle`, `view`)
- **Biometrics**: AndroidX Biometric 1.1.0 (`BiometricPrompt` & `BiometricManager`)
- **System Services**:
  - Android Accessibility Service (`typeWindowStateChanged | typeWindowsChanged`)
  - Android Notification Listener Service (`NotificationListenerService`)
  - System Alert Window (`TYPE_APPLICATION_OVERLAY`)
- **Storage**: Native `org.json` with thread-safe `SharedPreferences` serialization

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or command-line SDK tools
- JDK 17 or JDK 21
- Android SDK with API 34+

### Clone & Build
```bash
git clone https://github.com/AtharvaBari/onelock.git
cd onelock

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions Used

- `SYSTEM_ALERT_WINDOW`: Display the One UI Keyguard overlay over locked apps.
- `BIND_ACCESSIBILITY_SERVICE`: Multi-window application detection and Anti-Tamper inspection.
- `BIND_NOTIFICATION_LISTENER_SERVICE`: Notification content privacy masking.
- `CAMERA`: Silent intruder selfie capture on repeated failed attempts.
- `USE_BIOMETRIC`: Fast unlock with fingerprint / face sensors.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Keep protection active in Samsung One UI Deep Sleep.
- `QUERY_ALL_PACKAGES`: Retrieve user-installed launcher apps.

---

## 📄 License
This project is licensed under the Apache License 2.0.
