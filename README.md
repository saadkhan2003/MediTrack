<div align="center">

# 💊 MediTrack

### Smart Medicine Reminder & Dose Tracker

**A production-ready native Android application for managing medicine schedules, tracking dose adherence, and receiving smart reminders — with an optional Firebase cloud sync.**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/Min%20API-26-orange.svg)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

</div>

---

## 📱 Features

| Feature | Description |
|---|---|
| 💊 **Medicine Management** | Add, edit, and delete medicines with dosage, frequency, colour labels, and stock tracking |
| ⏰ **Smart Reminders** | Exact alarm scheduling with actionable notifications (Taken / Missed inline buttons) |
| 📋 **Dose History** | Browse all dose logs with date-range and status filters |
| 📊 **Adherence Reports** | Monthly adherence %, daily bar charts, and per-medicine breakdown |
| 📦 **Stock Tracking** | Auto-decrement stock on intake; refill alerts when below threshold |
| 🔕 **Missed Dose Detection** | Auto-marks a dose as missed 60 minutes after its scheduled time |
| 🔄 **Boot Persistence** | Reschedules all alarms after device reboot, OTA update, or timezone change |
| 🌙 **Theme & Font** | Light / Dark / System theme, three font-size presets |
| ☁️ **Cloud Sync (optional)** | Firebase Auth + Firestore real-time sync with offline-first local Room DB |
| 📤 **Data Export** | Share adherence reports as plain text via Android Share Sheet |

---

## 🎨 Design System

MediTrack features a premium, modern UI built with Jetpack Compose Material 3:

- **Glassmorphism** — Semi-transparent cards with soft borders and blur-style layering
- **Custom Floating Nav Bar** — Pill-shaped bottom navigation with animated tab indicators
- **Smooth Transitions** — Slide + fade animations between all screens
- **Dynamic Theming** — System-aware dark/light mode with a curated colour palette
- **Premium Typography** — Rounded, modern type scale for readability

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture |
| Database | Room (SQLite) |
| Dependency Injection | Hilt |
| Navigation | Jetpack Navigation Compose + Animated transitions |
| Async | Kotlin Coroutines + StateFlow |
| Alarms | AlarmManager (Exact) |
| Charts | Vico (Compose M3) |
| Cloud (optional) | Firebase Auth + Firestore |
| Min SDK | 26 (Android 8.0 Oreo) |
| Target SDK | 34 (Android 14) |

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────┐
│                  Presentation                     │
│  Screens (Compose)  │  ViewModels  │  Components │
│  LoginScreen        │  HomeVM      │  MedicineCard│
│  RegisterScreen     │  HistoryVM   │  BottomNavBar│
│  HomeScreen         │  ReportVM    │  DoseLogItem │
│  AddMedicineScreen  │  SettingsVM  │              │
│  HistoryScreen      │  AuthVM      │              │
│  ReportScreen       │              │              │
│  SettingsScreen     │              │              │
├──────────────────────────────────────────────────┤
│                    Domain                         │
│  Use Cases            │    Models                 │
│  AddMedicineUseCase   │    Medicine               │
│  LogDoseUseCase       │    DoseLog                │
│  GetAdherenceReport   │    DoseStatus, Frequency  │
├──────────────────────────────────────────────────┤
│                     Data                          │
│  Repositories         │  Room DAOs  │  Entities   │
│  MedicineRepository   │  MedicineDao│  Medicine   │
│  DoseLogRepository    │  DoseLogDao │  DoseLog    │
│  FirestoreSyncService │             │             │
├──────────────────────────────────────────────────┤
│               System Services                     │
│  AlarmScheduler  │  AlarmReceiver  │  BootReceiver│
│  NotificationHelper  │  MissedDoseReceiver        │
└──────────────────────────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio **Hedgehog (2023.1.1)** or later
- Android device or emulator running **API 26+**
- JDK 17

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/MediTrack.git
cd MediTrack
```

### 2. Open in Android Studio

- Select **File → Open** and navigate to the `MediTrack` folder
- Let Gradle sync automatically

### 3. Run the app

```bash
./gradlew installDebug
```

Or press ▶️ in Android Studio with a device/emulator connected.

---

## 🔥 Firebase Setup (Optional)

Firebase is entirely optional. The app works fully offline without any Firebase configuration.

To enable cloud sync and authentication:

1. Go to [Firebase Console](https://console.firebase.google.com) and create a project
2. Add an Android app with package name: `com.meditrack.app`
3. Download `google-services.json` and place it at: `app/google-services.json`
4. Enable **Email/Password Auth** in Firebase Console → Authentication
5. Enable **Firestore** and set the following security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

6. Run Gradle sync and rebuild

> If `google-services.json` is absent, the app runs in local-only mode automatically.

---

## 🔋 Battery Optimization (Important for Alarms)

For reliable reminders, disable battery optimization for MediTrack:

1. **Settings → Apps → MediTrack → Battery → Unrestricted**
2. Samsung users: **Settings → Battery → App Power Management → Never sleeping apps → Add MediTrack**

The app will prompt for this on first launch.

---

## 🔐 Permissions

| Permission | Purpose |
|---|---|
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after device reboot |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Accurate medicine-time alarms |
| `POST_NOTIFICATIONS` | Reminder and refill notifications (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Reliable background alarms on OEM devices |
| `VIBRATE` | Haptic notification alerts |
| `WAKE_LOCK` | Ensure alarm work completes |
| `INTERNET` | Optional Firebase sync/auth |
| `FOREGROUND_SERVICE` | Background reliability |

---

## 📂 Project Structure

```
MediTrack/
├── app/
│   ├── src/main/java/com/meditrack/app/
│   │   ├── alarm/               # AlarmScheduler, AlarmReceiver, BootReceiver
│   │   ├── data/
│   │   │   ├── local/           # Room database, DAOs, Entities
│   │   │   ├── preferences/     # DataStore preferences
│   │   │   ├── repository/      # Repository implementations
│   │   │   └── sync/            # FirestoreSyncService
│   │   ├── di/                  # Hilt modules
│   │   ├── domain/
│   │   │   ├── model/           # Medicine, DoseLog, enums
│   │   │   └── usecase/         # Business logic use cases
│   │   ├── notification/        # NotificationHelper, ActionReceiver
│   │   └── presentation/
│   │       ├── components/      # MedicineCard, BottomNavBar, DoseLogItem
│   │       ├── navigation/      # NavGraph, Screen definitions
│   │       ├── screens/
│   │       │   ├── auth/        # LoginScreen, RegisterScreen, AuthViewModel
│   │       │   ├── home/        # HomeScreen, HomeViewModel
│   │       │   ├── addmedicine/ # AddMedicineScreen, AddMedicineViewModel
│   │       │   ├── history/     # HistoryScreen, HistoryViewModel
│   │       │   ├── report/      # ReportScreen, ReportViewModel
│   │       │   └── settings/    # SettingsScreen, SettingsViewModel
│   │       └── theme/           # Color, Type, Theme
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── res/                 # Strings, drawables, icons
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## ⚠️ Known Limitations

- OEM background restrictions (especially on Xiaomi, Huawei, Samsung OneUI) may delay alarms even with optimizations disabled.
- Firebase Firestore cloud sync depends on security rules being correctly configured.
- Orphaned dose logs (for deleted medicines) in Firestore are skipped on pull — they don't cause crashes.

---

## 📄 License

```
MIT License

Copyright (c) 2025 MediTrack

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
