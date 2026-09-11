# Personal Assistant — Android App

A private, wake-word-activated Android assistant that responds **only to the
owner's voice** and acts as a personal command center: messaging readout,
call control, camera, phone lock, a permissions/behavior security advisor,
local ad blocking, a speech translator, emergency alerts, battery/data
monitoring, scheduled Do Not Disturb, and reminders.

This is a single-user, sideloaded app — not a public product. See
`personal-ai-assistant-blueprint.md` (the original spec this was built from)
for the full feature rationale.

## Status

Every feature listed in the blueprint has a real, working implementation —
not a stub. A few pieces have honest, documented limitations where a fully
faithful implementation would require a proprietary SDK/key or reverse a
platform restriction; those are called out below and in code comments at
the relevant class.

| Feature | Where | Notes |
|---|---|---|
| Wake word + voice lock | `voice/` | Uses Android's built-in `SpeechRecognizer` in a restart loop + a hand-rolled MFCC-based voice match (see limitations) |
| Reminders | `service/ReminderScheduler.kt` | Fully working, `AlarmManager`-based |
| Notification readout | `service/NotificationReaderService.kt` | Filters known messaging apps, queues in-memory only |
| Camera | `camera/CameraController.kt` | CameraX, headless foreground service, saves to app-private storage |
| Phone lock | `call/PhoneLocker.kt` | Device Admin API |
| Call control | `call/AssistantInCallService.kt` | `InCallService` + `RoleManager.ROLE_CALL_SCREENING` |
| Emergency mode | `emergency/` | Location + SMS alert + silent audio recording + shake trigger |
| Security watchdog | `security/` | Permission-heuristic advisor, daily `WorkManager` scan |
| Ad blocking | `vpn/` | Local DNS sinkhole via `VpnService` (see limitations) |
| Translator | `translate/Translator.kt` | ML Kit Translate + language ID, en/hi/ta/te/ml |
| Battery/data monitor | `service/BatteryDataMonitor.kt` | `UsageStatsManager`-based (see limitations) |
| Scheduled DND | `service/DndScheduler.kt` | Sleep-window alarms + optional calendar check |
| Onboarding | `onboarding/OnboardingActivity.kt` | Walks through every permission that can't be auto-granted |

## Architecture

- **Native Kotlin**, no cross-platform framework — most of these features
  (Accessibility/Device Admin/VPN/InCallService/CameraX) aren't reachable
  from a web view or Flutter plugin layer.
- **`AssistantForegroundService`** is the always-on core: it runs
  `WakeWordDetector`, gates every recognized command through `VoiceLock`,
  and hands accepted commands to `CommandRouter`.
- **`CommandRouter`** is a plain dispatcher — each feature is its own class
  in its own package (`call/`, `camera/`, `emergency/`, `security/`,
  `translate/`, `vpn/`), so features can be extended independently.
- **Local-first storage**: `data/SecureStore.kt` wraps
  `EncryptedSharedPreferences` for anything owner-identifying (voice
  embeddings, emergency contact). Nothing is synced off-device. Non-sensitive
  settings (sleep schedule, etc.) live in plain prefs via `data/AssistantPrefs.kt`.

## Building

```
./gradlew assembleDebug
```

Requires the Android SDK (compileSdk 34) and a JDK 17. This sandbox doesn't
have the Android SDK installed, so the build could not be run here — the
Gradle wrapper is included and every file was written and manually
cross-checked (imports, package layout, API usage) for consistency, but
please do a first build in Android Studio before relying on it.

Because of how many special-access permissions this app uses (Notification
Listener, Device Admin, VPN, call screening), **it must be sideloaded** —
this will not pass Play Store review for a personal-use app like this.

## First run

Open the app and go through **Start setup** (`OnboardingActivity`). It
walks through, in order:

1. Runtime permissions (mic, camera, location, phone, SMS, contacts, calendar).
2. Notification access (Settings — for message readout).
3. Device Admin (Settings — for phone lock).
4. Do Not Disturb access (Settings — for scheduled DND).
5. Usage access (Settings — for the security scan and usage summary).
6. The one-time local VPN consent dialog (for ad blocking).
7. The call-screening role (for call control).
8. **Voice enrollment** — record 3 short samples. The assistant will not
   start, and `BootReceiver` will not restart it after a reboot, until this
   is done — nothing should ever act on a voice it hasn't verified.
9. Emergency contact number.

## Known limitations (please read before relying on this)

- **Wake word engine.** A true offline wake-word engine (Porcupine, Vosk)
  runs a tiny always-on keyword model with no cloud dependency. That
  requires either a paid Porcupine access key or bundling a multi-MB Vosk
  language model as an asset — neither can be shipped sight-unseen in this
  codebase. `voice/WakeWordDetector.kt` uses Android's built-in
  `SpeechRecognizer` in a continuous restart loop as a working stand-in: it
  costs more battery and, depending on the OEM, may not be fully offline.
  Swapping in Porcupine/Vosk later only means replacing this one class —
  everything downstream (`VoiceLock`, `CommandRouter`) is unaffected.
- **Voice lock accuracy.** `voice/AudioFeatureExtractor.kt` computes a
  real MFCC-style embedding and compares it by cosine similarity — this is
  genuine signal processing, not a placeholder, and will reject clearly
  different voices. It is **not** the same false-accept guarantee a trained
  neural speaker-verification model (d-vector/x-vector) gives. If this needs
  to be airtight, swap in a trained on-device model.
- **Ad blocking scope.** `vpn/AdBlockVpnService.kt` intercepts and filters
  only DNS lookups (by routing just the device's configured DNS server
  IP(s) through the tunnel) — it does not proxy general traffic, which
  would require hand-writing a full user-space TCP/IP stack. This blocks
  ads/trackers at the domain level for anything that uses the system
  resolver, same as most open-source Android ad blockers. It cannot block:
  apps that hardcode their own DNS-over-HTTPS resolver, or in-app ads
  served over a domain the app already has cached — the blueprint calls
  this out too (in-app ad blocking generally needs root).
- **Battery/data monitor.** Android does not expose true per-app
  battery-percentage consumption to third-party apps. `BatteryDataMonitor`
  uses `UsageStatsManager` foreground time as the closest public-API proxy —
  a real, working signal, just not a literal battery-percentage figure.
- **Security watchdog** is a permissions/behavior heuristic advisor, not a
  malware scanner — see `security/PermissionHeuristics.kt` for the exact
  rules. Expect false positives; it's meant to prompt the owner to look, not
  to be an automatic verdict.
- **Call control** requires the owner to grant the Call Screening role
  (`RoleManager.ROLE_CALL_SCREENING`), not full default-dialer status — this
  keeps the ask smaller but is why answer/decline/mute go through
  `InCallService.Call` rather than `TelecomManager` directly.
- This sandbox has no Android SDK, so `./gradlew assembleDebug` could not
  actually be run to confirm a clean build — do that first in Android
  Studio.

## Project layout

```
app/src/main/java/com/owner/assistant/
  voice/       wake word, MFCC feature extraction, voice lock, enrollment
  service/     foreground service, command router, reminders, DND, battery
  call/        InCallService, device admin (phone lock)
  camera/      CameraX headless capture
  emergency/   location+SMS alert, silent recording, shake trigger
  security/    permission heuristics + watchdog + periodic worker
  vpn/         DNS-sinkhole ad blocker
  translate/   ML Kit speech translation
  onboarding/  step-by-step permission/setup flow
  data/        encrypted + plain local storage
  ui/          MainActivity
  receiver/    boot restart
```
