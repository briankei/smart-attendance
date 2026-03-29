# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NFC/QR Code Smart Student Attendance PWA (v7.6) by BrianKei. A local-first, zero-build Progressive Web App for classroom attendance tracking using Web NFC and QR codes. Licensed under a custom non-commercial license.

## Architecture

**Zero-build single-page app** — all application logic lives in `index.html` (~2800 lines of embedded HTML/CSS/JS). No frameworks, no bundler, no package.json.

Key files:
- `index.html` — entire frontend app (UI, state management, NFC/QR scanning, encryption, data import/export)
- `sw.js` — Service Worker for offline caching (version string must match app version)
- `manifest.json` — PWA manifest
- `server.py` — optional Python 3 HTTPS server for data storage (CSV-based, TLS required)
- `decrypt.html` — standalone utility for decrypting exported attendance data
- `deploy/` — GitHub Pages deployment directory (mirrors main files + `.nojekyll`)

## Running Locally

```bash
# Serve with Python HTTPS server (expects certs in ../certs/)
python3 server.py
# HTTP:8090 redirects to HTTPS:8443
```

No build step, no test runner, no linter configured. The app can also run from any static HTTPS server.

## Key Technical Patterns

- **All state in localStorage**: courses stored under `nfc_attendance_courses`, settings under `nfc_attendance_settings`, professor auth under `nfc_professor`, encryption key under `nfc_enc_key`
- **AES-GCM encryption**: NFC serial numbers are encrypted before storage using Web Crypto API. Functions: `encryptSerial()` / `decryptSerial()`
- **Professor auth gate**: `requireAuth(action, callback)` wraps sensitive operations — supports both NFC card and password authentication
- **Service Worker versioning**: `sw.js` cache version must be bumped alongside app version changes to invalidate stale caches
- **NFC mismatch re-registration**: 3-strike system — if a scanned NFC serial doesn't match the registered one 3 times, the user is prompted to re-register

## Data Model

```
Course { title, professor, professorTimestamps[], students[] }
Student { name, studentNo, last4, nfcSerial (encrypted), consented, consentedAt, timestamps[] }
```

Timestamps carry remarks: `nfc`, `qr`, `manual` to track attendance method.

## Modes

Attend NFC (continuous scan), Attend Code (last-4-digit verification), Attend QR (camera + verification), Find (locate student by NFC), Games (wheel/groups/random/timer).

## Deployment

The `deploy/` directory is a separate git repo pushed to GitHub Pages. When updating, files must be manually mirrored from root to `deploy/`. The `deploy` file at root appears to be a script/flag for this process.

## Browser Requirements

Chrome 89+ on Android 10+. HTTPS required for Web NFC API access. Camera permission needed for QR mode. Speech synthesis used for audio announcements.
