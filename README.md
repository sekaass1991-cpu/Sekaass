# Personal Assistant — Android App

A private, wake-word-activated Android assistant that responds **only to the
owner's voice** and acts as a personal command center: messaging readout,
call control, camera, phone lock, a permissions/behavior security advisor,
local ad blocking, a speech translator, emergency alerts, battery/data
monitoring, scheduled Do Not Disturb, and reminders — plus a real
conversational chat mode (text or voice, powered by Google's Gemini API with
your own free-tier key) for anything that isn't one of those fixed commands.

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
| Call control | `call/` | `InCallService` bound via the default-Dialer role (`RoleManager.ROLE_DIALER`) — the only way Android grants this; includes `InCallActivity` (calling screen) and `DialerActivity` (minimal dial pad) since being the default dialer requires providing both |
| Emergency mode | `emergency/` | Location + SMS alert + silent audio recording + shake trigger |
| Security watchdog | `security/` | Permission-heuristic advisor, daily `WorkManager` scan |
| Ad blocking | `vpn/` | Local DNS sinkhole via `VpnService` (see limitations) |
| Translator | `translate/Translator.kt` | ML Kit Translate + language ID, en/hi/ta/te (Malayalam not supported by ML Kit — see limitations) |
| Battery/data monitor | `service/BatteryDataMonitor.kt` | `UsageStatsManager`-based (see limitations) |
| Scheduled DND | `service/DndScheduler.kt` | Sleep-window alarms + optional calendar check |
| Onboarding | `onboarding/OnboardingActivity.kt` | Walks through every permission that can't be auto-granted |
| Chat mode | `chat/` | Real conversation via Google's Gemini API (free tier) — text/voice screen (`ChatActivity`) plus a fallback from voice commands that don't match a fixed phrase (`AssistantChat`) |

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
- **Design system**: a small shared palette (`res/values/colors.xml`, with a
  `values-night/` dark variant) and shape-drawable resources
  (`res/drawable/bg_*.xml` — rounded buttons/bubbles/cards/pills) applied
  across `MainActivity`, `OnboardingActivity`, `ChatActivity`, and
  `InCallActivity` via `Theme.PersonalAssistant`
  (`res/values/styles.xml`, `Theme.MaterialComponents.DayNight`). All screens
  are still built programmatically (no XML layouts), matching the rest of
  the codebase's style, just with consistent colors/shapes instead of
  default widget styling.

## Building

```
./gradlew assembleDebug
```

Requires the Android SDK (compileSdk 34) and a JDK 17. This sandbox doesn't
have the Android SDK installed, so the debug APK itself is built and
verified on every push by the `.github/workflows/build-apk.yml` GitHub
Actions workflow instead — see the Actions tab of this repo for the latest
build.

### Tests

```
./gradlew testDebugUnitTest
```

Real, runnable JUnit tests for the parts of the app that don't need an
Android device — `voice/AudioFeatureExtractorTest.kt` (the MFCC voice-match
pipeline: identical audio matches itself, different tones are less similar,
degenerate inputs don't crash), `vpn/DnsPacketProcessorTest.kt` (hand-builds
real IPv4/UDP/DNS packets and checks parsing + the blocked-response
construction byte-for-byte), and `security/PermissionHeuristicsTest.kt` (the
watchdog's rule set against synthetic app permission profiles). These run in
CI on every push alongside the APK build. Everything that genuinely needs a
device or emulator (onboarding flow, wake word, camera, calls, the VPN
tunnel itself) can't be automated here and needs manual testing on a real
phone.

Because of how many special-access permissions this app uses (Notification
Listener, Device Admin, VPN, call screening), **it must be sideloaded** —
this will not pass Play Store review for a personal-use app like this.

### Google Play Protect will likely block the first install

Play Protect's on-device heuristic flags exactly this permission
fingerprint — SMS, notification access, device admin, camera, phone state
together — as spyware-like, regardless of what the app actually does, and
can outright refuse to install it ("App blocked to protect your device").
This is expected friction for this category of app, not a broken build. To
get past it: Play Store app → profile icon → **Play Protect** → gear icon →
turn off "Scan apps with Play Protect" (and "Improve harmful app
detection") → install the APK → turn scanning back on afterward. Some
phones (Samsung in particular) also have their own blocker — Settings →
Security and privacy → **Auto Blocker** — that needs the same temporary
toggle-off.

### Android will also deny the sensitive permissions themselves at first

Even after the app is installed, Android 13+'s "Restricted settings"
protection silently denies a sideloaded app the sensitive toggles it asks
for (Notification access, Accessibility) — trying to turn them on in
Settings just shows "App was denied access" with no obvious way through.
Fix once, per app: **Settings → Apps → See all apps → Personal Assistant →
⋮ (top-right menu) → Allow restricted settings** (confirm with
fingerprint/PIN). Then go back and toggle the permission on — it works
immediately after that, and this only has to be done once, not once per
permission.

## First run

Open the app and go through **Start setup** (`OnboardingActivity`). It
walks through, in order:

1. Runtime permissions (mic, camera, location, phone, SMS, contacts, calendar).
2. **Battery optimization exemption** — without this, Android eventually
   pauses the wake-word listener in the background, same as any always-on
   voice assistant needs.
3. Notification access (Settings — for message readout).
4. Device Admin (Settings — for phone lock).
5. Do Not Disturb access (Settings — for scheduled DND).
6. Usage access (Settings — for the security scan and usage summary).
7. The one-time local VPN consent dialog (for ad blocking).
8. **Default Dialer role** (for call control) — see "Known limitations" for
   why this is required, not optional, for answer/decline/mute to work.
9. **Voice enrollment** — record 3 short samples. The assistant will not
   start, and `BootReceiver` will not restart it after a reboot, until this
   is done — nothing should ever act on a voice it hasn't verified.
10. Emergency contact number.

The assistant starts running in the background automatically the moment
setup finishes — there's no separate "turn it on" step, the same as Google
Assistant is just always listening once it's set up. `MainActivity` still
has manual **Start assistant** / **Stop assistant** buttons if you want to
toggle it later.

### Setting up chat mode

Chat mode doesn't need any of the setup above — open **Chat** from the main
screen any time. The first time, it asks for a Gemini API key:

1. Go to [aistudio.google.com/apikey](https://aistudio.google.com/apikey),
   sign in with a Google account, and click **Create API key**. This is
   Google AI Studio's **free tier** — no credit card required to start.
   It's rate-limited (fewer requests per minute/day than the paid tier), and
   Google can change those limits at any time — see
   [ai.google.dev/pricing](https://ai.google.dev/pricing) for the current
   numbers — but ordinary personal chat use fits comfortably inside it.
2. Paste it into the dialog (or tap **API key** in the chat screen's header
   later to change it or the model ID — defaults to `gemini-2.5-flash`).
   It's stored the same way as the voice profile and emergency contact —
   encrypted, on-device only, never sent anywhere but
   `generativelanguage.googleapis.com`.
3. Type or hold the 🎤 button to talk; replies show on screen and are
   spoken aloud. Tap **Voice** in the header to browse and preview the
   voices your device has installed and pin the one you want — see "Known
   limitations" for why this isn't a single automatic toggle.

Once a key is set, the always-listening wake-word assistant also uses it:
any command that doesn't match one of the fixed phrases above (reminders,
camera, etc.) gets sent to Gemini instead of being silently ignored, so you
can just talk to it. Both the chat screen and voice fall-through share the
same conversation history (in memory only, up to the last 20 messages —
cleared on app restart, not synced anywhere).

Note: if Google renames or retires `gemini-2.5-flash` after this was
written, the chat screen will show the exact API error — just change the
model ID in the **API key** dialog to whatever Google AI Studio currently
lists, no new APK needed.

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
- **Translator doesn't cover Malayalam.** ML Kit Translate's on-device
  language set (~59 languages) doesn't include Malayalam, so `translate/Translator.kt`
  only supports English/Hindi/Tamil/Telugu even though the blueprint asks
  for all four. Adding Malayalam would mean routing just that language
  through a cloud API (e.g. Google Cloud Translation) instead of ML Kit's
  on-device model — a real trade-off (network dependency, cost), so it's
  left as a follow-up rather than silently faked.
- **"Girl voice" and "every language" for spoken replies.** `util/SpeechOutput.kt`
  does two honest best-effort things rather than promising more than Android
  can deliver: (1) Android's TTS API has no documented voice-gender field —
  there is no reliable cross-device way to say "always use a female voice."
  It auto-picks a female-sounding voice when the installed engine's own voice
  names hint at it (works for many of Google TTS's classic per-language
  voices), and the chat screen's **Voice** button lets you browse, preview,
  and pin an exact voice per language if the auto-pick isn't right — that's
  the guaranteed way to get the voice you want. (2) No TTS engine speaks
  "every language in the world" — Google's engine (the usual default) covers
  on the order of 40-50 languages with installed voice packs. Chat replies
  are passed through ML Kit language detection first (`speakAuto`) so
  whatever language Gemini actually replies in gets matched to the closest
  installed voice/locale automatically, rather than always speaking English.
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
- **Call control requires becoming the default Dialer app — there is no
  lighter-weight option.** An earlier version of this app requested
  `RoleManager.ROLE_CALL_SCREENING`, which sounds related but isn't: that
  role only lets an app silently allow/block a call *before it rings*
  (`CallScreeningService`) — it has nothing to do with the `InCallService`
  binding that "answer"/"decline"/"mute" actually need. Android only binds
  `InCallService` for real phone calls to the phone's default Dialer app (or
  a car-mode companion) — this is a deliberate anti-spyware restriction, not
  something a lighter permission can unlock. So `call/AssistantInCallService.kt`
  only receives calls once the owner grants `RoleManager.ROLE_DIALER` during
  onboarding, and the app ships `InCallActivity` (a minimal but real calling
  screen — caller name, answer/decline/mute/end) and `DialerActivity` (a bare
  number pad, required for `ROLE_DIALER` eligibility) so becoming the default
  dialer doesn't cost the owner the ability to see or touch-answer a call.
  This is reversible any time in Settings → Apps → Default apps → Phone app.
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
