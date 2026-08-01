# TaxTitan Consultancy

A practice management app for a Chartered Accountant / accounting firm, with
separate Admin and Client portals. Built with React, TypeScript, Vite, and
Tailwind CSS. Data is persisted entirely in the browser's `localStorage` —
there is no backend.

## Features

**Admin Portal** — dashboard with revenue chart and upcoming deadlines,
client management, invoices (with an advanced billing/aging report), GST/TDS/
ROC/audit compliance tracking, ledger and financial statements, client
account statements, document review, messaging, notifications, an audit
trail, reminders, an AI assistant, a service catalog, a worklist, portal
credential storage, and firm settings.

**Client Portal** — personal dashboard, document upload, messaging with the
firm, notifications, a service catalog to request services, an AI assistant,
and account settings.

## Getting started

```bash
npm install
npm run dev
```

Open the printed local URL and sign in with one of the demo accounts (or use
the "Demo Admin" / "Demo Client" buttons on the login screen):

- Admin: `admin@taxtitan.com` / `admin123`
- Client: `arjun.kumar@example.com` / `client123`

New clients can also self-register from the Client Portal login screen.

## Android app

The web app is wrapped as a native Android app with [Capacitor](https://capacitorjs.com).

**Get a build without installing anything:** push to GitHub (or run the
"Build Android APK" workflow manually from the Actions tab) and download the
`taxtitan-consultancy-debug-apk` artifact once it finishes — that's a
GitHub-hosted build with full internet access, so it doesn't need anything
installed locally.

**Build locally** (needs Android Studio / the Android SDK installed):

```bash
npm run android:build   # builds the web app, syncs it into android/, and assembles the debug APK
# APK lands at android/app/build/outputs/apk/debug/app-debug.apk
```

Or open the native project in Android Studio with `npm run android:open` and
run it from there. Whenever you change the web app, re-run `npm run
android:sync` before rebuilding so the native project picks up the latest
build.

## Notes

- All data lives in `localStorage` under the `sekaass:` prefix; clearing
  browser storage resets the app back to its seed data. On Android this is
  scoped to the app's own WebView storage, separate from any browser.
- The AI Assistant gives simple data-aware summaries out of the box. Add a
  Claude API key under Admin → Settings → AI Assistant to have it answer for
  real (see the in-app notice there about the key being visible client-side,
  since this app has no backend).
- Client-facing notifications (new invoices, compliance deadlines, document
  status changes, messages, and the new-client welcome email) also send a
  real email via [EmailJS](https://www.emailjs.com) once you add your Service
  ID, Template ID, and Public Key under Admin → Settings → Email. Your
  EmailJS template needs `to_email`, `to_name`, `from_name`, `subject`, and
  `message` variables. Use "Send Test Email" there to verify it's working.
