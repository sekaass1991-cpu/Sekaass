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

## Notes

- All data lives in `localStorage` under the `sekaass:` prefix; clearing
  browser storage resets the app back to its seed data.
- The AI Assistant gives simple data-aware summaries out of the box. Add an
  API key and endpoint under Admin → Settings → API Settings to connect it to
  a real chat-completions-compatible API.
