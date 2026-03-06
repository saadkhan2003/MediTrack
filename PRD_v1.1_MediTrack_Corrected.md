# 🧠 MediTrack PRD v1.1 (Corrected & Implementation-Ready)

## Purpose
This document is a corrected version of the original PRD, with conflicting requirements resolved and missing production-critical details added. It is optimized for agentic code generation (Cursor/Windsurf/Copilot/Bolt/v0/Lovable) and intended to produce a compilable, runnable Android app.

---

## ⚡ Master Prompt (Use This)

```text
You are a senior Android engineer and mobile architect. Build a complete, production-ready native Android app named "MediTrack – Smart Medicine Reminder & Dose Tracker" from scratch using Kotlin + Jetpack.

Do not ask clarifying questions. Do not leave placeholders, TODOs, or stubs. Implement every required file fully.

Use these constraints exactly:
- Min SDK 26, Target SDK 34
- Kotlin + Jetpack Compose (Material 3)
- MVVM + Clean Architecture (Repository Pattern)
- Room + Coroutines + Flow
- Hilt for dependency injection
- Gradle Kotlin DSL

If any requirement in this prompt appears ambiguous, prefer the explicit "Resolved Decisions" section below.
```

---

## ✅ Resolved Decisions (Authoritative)

1. **Alarm requestCode formula (authoritative):**
   - Use `requestCode = medicineId * 10000 + dayOfYear * 10 + slotIndex`.
   - This supersedes any older/simple formula and prevents collisions across day slots.

2. **Non-dismissable reminder notification behavior:**
   - For dose reminders, use `setOngoing(true)` and `setAutoCancel(false)` until TAKEN/MISSED action is tapped.

3. **Exact alarm permission handling (Android 12+):**
   - Do **not** use runtime permission API for `SCHEDULE_EXACT_ALARM`.
   - Check `AlarmManager.canScheduleExactAlarms()`.
   - If false, deep-link to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.

4. **Notification permission (Android 13+):**
   - Request `POST_NOTIFICATIONS` via runtime permission API.
   - If denied, show rationale and settings deep-link.

5. **Missed auto-update alarm contract:**
   - For each scheduled dose alarm, schedule a second alarm at `scheduled + 60min`.
   - If matching log remains `PENDING`, update to `MISSED`.

6. **Settings source of truth:**
   - Profile fields (`displayName`, `email`, `dob`) in Room (`users` table).
   - App preferences (`themeMode`, `fontSize`, `notificationsEnabled`, `reminderLeadMinutes`) in DataStore.
   - UI combines both via ViewModel.

7. **Firebase optionality:**
   - Include dependencies and `google-services` wiring behind build flag `FEATURE_FIREBASE=false` by default.
   - App must run without Firebase configured.

8. **Deep-link highlight contract:**
   - Home route supports optional arg: `home?highlightMedicineId={id}`.
   - Notification content intent opens this route.

---

## 📱 Project Overview
- **App Name:** MediTrack – Smart Medicine Reminder & Dose Tracker
- **Platform:** Android Native
- **Language:** Kotlin
- **Min SDK:** 26
- **Target SDK:** 34
- **Architecture:** MVVM + Clean Architecture + Repository
- **UI:** Jetpack Compose Material 3
- **Build:** Gradle Kotlin DSL

---

## 🗂️ Required Project Structure
Generate all files listed in the original structure, plus these required additions:

- `presentation/theme/` (Compose theme files):
  - `Color.kt`
  - `Type.kt`
  - `Theme.kt`

- Optional (recommended) worker package:
  - `worker/RescheduleAuditWorker.kt` (for “missing alarms” audit)

- Optional utility package:
  - `util/JsonConverters.kt` (time list conversion support)

All files must be fully implemented.

---

## 📦 Gradle Requirements
Use all dependencies from the original PRD and ensure these plugin/build details are present:

- App plugins:
  - `com.android.application`
  - `org.jetbrains.kotlin.android`
  - `org.jetbrains.kotlin.kapt`
  - `com.google.dagger.hilt.android`
  - `com.google.devtools.ksp` (optional if using KSP; if KSP used, do not also kapt Room compiler)
  - `com.google.gms.google-services` (only when Firebase feature flag enabled)

- `compileSdk = 34`
- Java/Kotlin target `17`
- Compose enabled + compiler extension configured
- Proguard rules and release build type configured

**Important:** pick **one** annotation path for Room/Hilt (`kapt` or `ksp`) and keep consistent.

---

## 🗄️ Data Model & Schema
Implement entities exactly as in the original PRD. Additional constraints:

- `scheduledTimes` in `MedicineEntity` stores JSON array of `HH:mm` strings.
- Add Room indices:
  - `medicines(isActive)`
  - `dose_logs(medicineId)`
  - `dose_logs(scheduledTime)`
  - `dose_logs(status)`

- `DoseLogEntity` uniqueness:
  - unique index on `(medicineId, scheduledTime)` to avoid duplicate PENDING entries from repeated alarms.

- Foreign key cascade from `dose_logs.medicineId -> medicines.id`.

---

## ⚙️ Alarm & Notification Core Contracts

### AlarmScheduler
```kotlin
class AlarmScheduler(private val context: Context) {
    fun scheduleDose(medicineId: Int, medicineName: String, triggerAtMillis: Long, slotIndex: Int)
    fun scheduleMissedCheck(medicineId: Int, medicineName: String, scheduledAtMillis: Long, slotIndex: Int)
    fun cancelAllForMedicine(medicineId: Int)
    fun rescheduleAll(medicines: List<Medicine>)
}
```

Rules:
- Use `setExactAndAllowWhileIdle()`.
- `PendingIntent` flags: `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.
- Encode `medicineId`, `medicineName`, `scheduledTime` extras.
- Request code for dose: `medicineId * 10000 + dayOfYear * 10 + slotIndex`.
- Request code for missed-check: above + `5` offset (or dedicated deterministic suffix).
- If `remainingStock == 0`, skip scheduling and expose UI badge state.

### AlarmReceiver
- Use `goAsync()` + coroutine on `Dispatchers.IO`.
- Insert/ensure `DoseLogEntity(PENDING)` for `(medicineId, scheduledTime)`.
- Show dose reminder notification.

### MissedCheckReceiver (required for auto-miss)
- Use `goAsync()` + IO dispatcher.
- If log still `PENDING` after 60 minutes, mark `MISSED`.

### AlarmBootReceiver
- Listen to:
  - `BOOT_COMPLETED`
  - `MY_PACKAGE_REPLACED`
  - `TIMEZONE_CHANGED`
- Use `goAsync()` and reschedule all active medicines.

### NotificationHelper
- Create channels:
  - `meditrack_reminders`
  - `meditrack_refills`
- Dose reminder notification:
  - action buttons: TAKEN / MISSED
  - ongoing true, autoCancel false
  - separate notifications per dose (no grouping)
- Refill alert notification for threshold crossing.

### NotificationActionReceiver
- Handle actions `ACTION_TAKEN`, `ACTION_MISSED`.
- Update log status and `loggedTime`.
- If TAKEN, decrement stock (non-negative floor).
- If stock <= threshold, trigger refill alert.
- Dismiss actioned notification.

---

## 🧾 Manifest Requirements (Complete)
Must include:
- Permissions:
  - `RECEIVE_BOOT_COMPLETED`
  - `SCHEDULE_EXACT_ALARM`
  - `USE_EXACT_ALARM`
  - `POST_NOTIFICATIONS`
  - `VIBRATE`
  - `WAKE_LOCK`
  - `INTERNET`
  - `FOREGROUND_SERVICE`
  - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

- Application declaration:
  - `android:name=".MediTrackApplication"`

- Main activity with launcher intent-filter.
- Receivers:
  - `.alarm.AlarmReceiver` (not exported)
  - `.alarm.MissedCheckReceiver` (not exported)
  - `.alarm.AlarmBootReceiver` (exported, with boot/package/timezone actions)
  - `.notification.NotificationActionReceiver` (not exported)

---

## 🖥️ Screen Requirements (Compose)
Implement all screens from the original PRD fully. Additional explicit constraints:

- Every interactive component has `contentDescription` where applicable.
- Minimum touch target 48dp.
- Status shown by both color and text/icon.
- Every screen composable has at least one `@Preview`.
- `HomeScreen` empty state includes CTA button to add medicine.
- `AddMedicineScreen` supports add/edit via optional medicineId.
- `HistoryScreen` supports status and date-range filters concurrently.
- `ReportScreen` export uses Android sharesheet with plain text report.
- `SettingsScreen` “Clear All Data” clears Room + DataStore atomically.

---

## 🧭 Navigation Requirements
Routes:
- `home?highlightMedicineId={id}`
- `add_medicine?medicineId={medicineId}`
- `history`
- `report`
- `settings`

Rules:
- Bottom bar shown on Home/History/Report/Settings only.
- AddMedicine is full screen without bottom bar.
- Notification tap deep-links to Home with `highlightMedicineId`.

---

## 🧪 ViewModel Contracts
Implement contracts from original PRD; add these guarantees:
- `StateFlow` for UI state + immutable UI models.
- One-shot events via `SharedFlow`.
- All Room I/O on `Dispatchers.IO`.
- Explicit `Result` handling with surfaced error messages.

Add missing enum/model:
- `Frequency` enum: `ONCE_DAILY`, `TWICE_DAILY`, `THREE_TIMES_DAILY`, `CUSTOM`.

---

## 🔁 Use Case Logic (Strict)

### LogDoseUseCase
- Update log status/time by `logId`.
- If `TAKEN`, decrement stock once.
- Emit refill event when threshold reached.
- Return sealed `Result` (`Success`, `Error`).

### GetAdherenceReportUseCase
- Input: date range millis.
- Include only TAKEN + MISSED.
- Group by day and by medicine.
- Compute adherence: `taken / (taken + missed) * 100` (0 when denominator=0).
- Return `Flow` for real-time updates.

---

## 🛡️ Edge-Case Requirements (Authoritative)
Implement all 10 from original PRD plus:

11. **Duplicate alarm protection:** unique `(medicineId, scheduledTime)` log row or upsert strategy.
12. **Alarm horizon policy:** schedule next 14 days rolling; refresh on app launch and daily worker.
13. **Force-stop recovery audit:** on app foreground, compare expected schedule vs upcoming alarms and reschedule missing ones.
14. **Clock change handling:** include `ACTION_TIME_CHANGED` in receiver handling.

---

## 🎨 Design System
Use the exact palette provided in original PRD. Do not add random colors.

Typography scaling behavior:
- `NORMAL` = 1.00x
- `LARGE` = 1.15x
- `EXTRA_LARGE` = 1.30x

Apply app-wide via composition-local scale strategy.

---

## 📄 README Requirements
Include all original sections plus:
- Permission rationale matrix (why each permission is required).
- Exact alarm setup instructions for Android 12+.
- Notification permission flow for Android 13+.
- Known limitations section.

---

## 🚀 Delivery Requirements (Non-Negotiable)
1. Every file exists and is non-empty.
2. No `TODO()`, no placeholders, no stubbed functions.
3. Project compiles on Android Studio Hedgehog+.
4. Use Coroutines + Flow (no LiveData unless forced by library).
5. Hilt for DI; avoid manual singleton wiring.
6. Receivers using background work must use `goAsync()` safely.
7. Handle all errors explicitly.
8. Room versioning + migration strategy required.
9. Firebase remains optional and app runs without it.
10. Include previews for all major screen composables.

---

## Quick Acceptance Checklist
- [ ] Build succeeds (`assembleDebug`)
- [ ] App launches and requests required runtime permissions
- [ ] Alarms trigger in Doze with exact scheduling path
- [ ] TAKEN/MISSED actions update DB and UI immediately
- [ ] Missed auto-update after +60 minutes works
- [ ] Refill alerts trigger at threshold
- [ ] Reboot/timezone/package replace causes rescheduling
- [ ] Deep-link highlight works from notification tap
- [ ] Clear-all-data wipes Room + DataStore
- [ ] Report chart and export output are correct

---

This v1.1 PRD is now the authoritative implementation contract for MediTrack.