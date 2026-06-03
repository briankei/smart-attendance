# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NFC/QR Code Smart Student Attendance PWA (v9.5) by BrianKei. A local-first, zero-build Progressive Web App for classroom attendance tracking using Web NFC, QR codes, and Bluetooth Low Energy (BLE). Ships both as a PWA and as a Capacitor-wrapped native Android app (the native wrapper enables BLE advertising/GATT server and native TTS). Licensed under a custom non-commercial license.

## Architecture

**Zero-build single-page app** — all primary application logic lives in `index.html` (~5900 lines of embedded HTML/CSS/JS). No frameworks, no bundler, no package.json in the web root. A Capacitor native Android project lives under `mobile/` and wraps the same web assets with a custom `BleAttendance` plugin.

Key files:
- `index.html` — entire frontend app (UI, state management, NFC/QR scanning, BLE handling, encryption, data import/export). Includes the inlined `qrcode-generator` library.
- `ble-attendance.js` — BLE helper module (legacy/alternate path using `@capacitor-community/bluetooth-le`); exposes `window.BLEAttendance`
- `ble-checkin.html` — standalone student check-in page using Web Bluetooth (reads GATT response from the professor device)
- `checkin.html` — legacy WiFi check-in page (button hidden in-app; code preserved)
- `qrcode-lib.js` — QR generator library (also inlined inside `index.html` for CSP compatibility under the Capacitor WebView)
- `sw.js` — Service Worker for offline caching (`CACHE_NAME` must match app version, e.g. `smart-attendance-v9.5`)
- `manifest.json` — PWA manifest
- `server.py` — optional Python 3 HTTPS server for data storage (CSV-based, TLS required)
- `decrypt.html` — standalone utility for decrypting exported attendance data
- `mobile/` — Capacitor Android project (native BLE peripheral/GATT server, native TTS, NFC bridging)
- `deploy/` — GitHub Pages deployment directory (mirrors main files + `.nojekyll`)

## Running Locally

```bash
# Serve with Python HTTPS server (expects certs in ../certs/)
python3 server.py
# HTTP:8090 redirects to HTTPS:8443
```

No build step, no test runner, no linter configured. The app can also run from any static HTTPS server.

## Key Technical Patterns

- **All state in localStorage**: courses under `nfc_attendance_courses`, settings under `nfc_attendance_settings`, professor auth under `nfc_professor`, encryption key under `nfc_enc_key`, BLE device→student map under `ble_registered_devices`
- **AES-GCM encryption**: NFC serial numbers are encrypted before storage using Web Crypto API. Functions: `encryptSerial()` / `decryptSerial()`
- **Professor auth gate**: `requireAuth(action, callback)` wraps sensitive operations — supports both NFC card and password authentication
- **Service Worker versioning**: `sw.js` cache version must be bumped alongside app version changes to invalidate stale caches
- **NFC mismatch re-registration**: 3-strike system — if a scanned NFC serial doesn't match the registered one 3 times, the user is prompted to re-register
- **BLE peripheral (native only)**: Professor side advertises as `SmartAttendance` via the Capacitor `BleAttendance` plugin. Students write `action|studentNo` to the write characteristic; the professor sets a **per-device** response on the read characteristic (`OK|Name|Message|Distance` or `ERROR|Message|Distance`). The per-device response map fixes concurrent check-ins; responses are cleared to `PENDING` on each new write.
- **RSSI distance estimation**: BLE events carry `rssi` and a human-readable `distanceLabel` that is logged alongside each check-in.
- **Capacitor detection**: `isCapacitorNative()` / `window.Capacitor.isNativePlatform()` gates BLE advertising, native TTS, and native file-share code paths.

## Data Model

```
Course { title, professor, professorTimestamps[], students[] }
Student { name, studentNo, last4, nfcSerial (encrypted), consented, consentedAt, timestamps[] }
```

Timestamps carry remarks recording the attendance method: `nfc`, `qr code`, `manual`, `ble|<deviceAddress>`, `wifi`, and wheel responses such as `wheel|Good`. The student-row UI normalises these to the tag set `nfc | qr code | manual | ble | wifi`.

BLE device registrations live in a separate localStorage map:
```
ble_registered_devices = { [deviceAddress: string]: studentId: number }
```

## Modes

Attend NFC (continuous scan), Attend Code (last-4-digit verification), Attend QR (camera + verification), **BLE Attend** (Capacitor-only; broadcasts as `SmartAttendance` and displays a check-in QR for `ble-checkin.html`), Find (locate student by NFC), Games (wheel/groups/random/timer). WiFi Check-in exists in the codebase but its button is currently hidden.

## Deployment

The `deploy/` directory is a separate git repo pushed to GitHub Pages. When updating, files must be manually mirrored from root to `deploy/`. The `deploy` file at root appears to be a script/flag for this process.

## Browser Requirements

Chrome 89+ on Android 10+. HTTPS required for Web NFC and Web Bluetooth. Camera permission needed for QR mode. Bluetooth + Location permissions needed for BLE student check-in. Speech synthesis used for audio announcements. The Capacitor native Android build is required for the professor-side BLE peripheral (GATT server) role.
