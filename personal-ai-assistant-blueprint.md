# Personal AI Assistant — Android App Blueprint

## 1. Overview
A private, wake-word-activated Android assistant that responds **only to the owner's voice**, controls key phone functions, and acts as a personal command center. Not a public product — single-user, locked to the owner.

**Target platform:** Android (native app, Kotlin, using Android's Accessibility Service, Notification Listener, and Speech APIs)

---

## 2. Core Activation
- **Wake word:** Custom phrase (e.g., "Hey Assistant") using an on-device wake-word engine (e.g., Porcupine or Vosk for offline detection).
- **Voice lock:** Uses speaker verification (voice embedding match) so only the owner's voice triggers commands — everyone else's speech is ignored even if they say the wake word.
- **Always-listening mode:** Runs as a foreground service with a persistent notification (required by Android for background mic access).

---

## 3. Feature List

### A. Messaging & Calls
- Read incoming WhatsApp/SMS notifications aloud via Notification Listener Service.
- Voice command to have messages summarized ("what messages did I get?").
- On incoming call: announce caller, accept voice commands — "answer", "decline", "mute" — routed through Android's `TelecomManager` / `InCallService`.

### B. Camera & Media
- "Take a picture" / "take a video" — triggers `CameraX` capture in background or foreground, saved to a private app folder.

### C. Phone Control
- "Lock my phone" — uses Device Admin API or Accessibility Service to trigger lock.
- "What are my notifications?" — reads current notification shade via Notification Listener.

### D. Security Watchdog
- Periodic scan of installed apps' permissions.
- Flags apps with excessive/unusual permission combos (e.g., flashlight app requesting SMS access).
- Voice/text alert: "This app looks unsafe, here's why."
- *Note: this is a permissions-and-behavior heuristic advisor, not a malware signature scanner — real antivirus engines require licensing and cannot be built from scratch safely.*

### E. Ad Blocking
- Local DNS-based ad blocking (e.g., integrate a local VPN service using Android's `VpnService` API, filtering against a known ad-domain blocklist).
- Cannot block in-app ads inside apps like YouTube without root — spec this limitation clearly for Claude Code to explain to the user.

### F. Translator
- Real-time speech-to-speech translation: Tamil, Hindi, Telugu, Malayalam ↔ English (or each other).
- Uses on-device or cloud translation API (e.g., Google Cloud Translation or ML Kit for offline).
- Still gated by the voice-lock — only owner's speech is translated/acted on.

### G. Emergency Mode
- Trigger phrase (e.g., "I'm in danger") or a hardware gesture (shake/power-button pattern).
- Sends current GPS location + an alert SMS/WhatsApp message to a pre-set emergency contact.
- Optionally starts silent audio/video recording.

### H. Battery & Data Saver
- Monitors background battery/data drain per app (via `UsageStatsManager`).
- Warns: "App X used 15% battery in the background today."

### I. Scheduled Do Not Disturb
- Auto-enables DND based on calendar events or a set sleep schedule.
- Integrates with Android's `NotificationManager` DND policy access.

### J. Reminders
- "Remind me at 5 for the meeting" — parsed via on-device NLU, scheduled with `AlarmManager`, delivered as voice + notification.

---

## 4. Permissions Required (Android Manifest)
- `RECORD_AUDIO` — wake word + commands
- `BIND_NOTIFICATION_LISTENER_SERVICE` — read notifications
- `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`, `MODIFY_PHONE_STATE` — call control
- `CAMERA` — photo/video
- `BIND_DEVICE_ADMIN` — phone lock
- `ACCESS_FINE_LOCATION` — emergency mode
- `BIND_VPN_SERVICE` — ad blocking
- `PACKAGE_USAGE_STATS` — battery/data monitor
- `SCHEDULE_EXACT_ALARM` — reminders

**Important:** Several of these (Notification Listener, Accessibility, Device Admin, VPN) require the user to manually enable them in Android Settings — they can't be auto-granted. Claude Code should build a clean onboarding flow that walks the owner through enabling each one.

---

## 5. Architecture Suggestion
- **App type:** Native Android (Kotlin) for deep system access — a web app or Flutter app can't reach most of these APIs.
- **Background engine:** Foreground service for always-on listening + wake word detection.
- **Local-first:** Store all personal data (voice profile, message logs, emergency contact) locally/encrypted — no cloud sync unless the owner explicitly wants backup.
- **Modular commands:** Build a command-router so each feature (call control, camera, etc.) is its own module, easy to extend later.

---

## 6. Suggested Build Order (for Claude Code)
1. Wake word + voice-lock foundation (core gate for everything else).
2. Reminders + DND scheduling (simplest, no special permissions beyond alarms).
3. Notification reading + camera + phone lock.
4. Call control.
5. Emergency mode.
6. Security watchdog.
7. Ad blocking (VPN service).
8. Translator.

---

## 7. Known Limitations to Flag to the User
- Google Play Store has strict policies on accessibility/call-control apps — this will likely need to be sideloaded (APK install), not published on Play Store.
- True antivirus-grade malware detection isn't realistically buildable from scratch; the security watchdog here is a permissions/behavior advisor.
- In-app ad blocking (inside apps, not browsers) generally requires root access, which this spec avoids for safety.
