# System Design Specification

**Project:** NFC/QR Code Smart Student Attendance PWA
**Version:** v8.4
**Last Updated:** 2026-04-03
**Author:** BrianKei (cwk)

---

## 1. System Overview

A Progressive Web App (PWA) for classroom attendance tracking using Web NFC cards and QR codes. The system is designed as a local-first, zero-build, single-page application with an optional Python HTTPS backend.

### 1.1 Design Principles

- **Local-first:** All data stored on device via localStorage; no cloud dependency
- **Zero-build:** No bundler, no framework, single HTML file with embedded CSS/JS
- **Privacy-first:** NFC serial numbers encrypted at rest using AES-GCM
- **Progressive enhancement:** Works offline as installed PWA; optional server mode
- **Platform-adaptive:** NFC on Android, Code/QR fallback on iOS

---

## 2. Architecture

### 2.1 High-Level Architecture

```
+-------------------------------------------+
|           Browser (Chrome 89+)            |
|  +-------------------------------------+  |
|  |         index.html (PWA)            |  |
|  |  +----------+ +------------------+  |  |
|  |  |  UI Layer| | State Management |  |  |
|  |  +----------+ +------------------+  |  |
|  |  +----------+ +------------------+  |  |
|  |  | Web NFC  | | Web Crypto API   |  |  |
|  |  | Web Cam  | | Speech Synthesis |  |  |
|  |  | QR Scan  | | Web Audio API    |  |  |
|  |  +----------+ +------------------+  |  |
|  +-------------------------------------+  |
|  +-------------------------------------+  |
|  |     Service Worker (sw.js)          |  |
|  |  Cache-first assets, network-first  |  |
|  +-------------------------------------+  |
|  +-------------------------------------+  |
|  |     localStorage / IndexedDB        |  |
|  +-------------------------------------+  |
+-------------------------------------------+
         |  (optional)
         v
+-------------------------------------------+
|     server.py (Python 3 HTTPS)            |
|  +----------+ +------------------------+  |
|  | HTTP(S)  | | CSV Data Storage       |  |
|  | Handler  | | Thread-safe file I/O   |  |
|  +----------+ +------------------------+  |
+-------------------------------------------+
```

### 2.2 Component Breakdown

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Frontend App | Vanilla HTML/CSS/JS | All UI and business logic |
| Service Worker | sw.js | Offline caching, asset versioning |
| PWA Manifest | manifest.json | Install-to-homescreen support |
| Encryption | Web Crypto API (AES-GCM) | Encrypt NFC serials in storage |
| NFC Reader | Web NFC API (NDEFReader) | Read NFC card serial numbers |
| QR Scanner | BarcodeDetector API + Camera | Read QR codes via camera |
| Audio | Web Audio API + Speech Synthesis | Beeps, announcements |
| Backend (optional) | Python 3, SimpleHTTPRequestHandler | HTTPS server, CSV storage |
| Decryptor | decrypt.html | Standalone page to decrypt exported data |

### 2.3 File Structure

```
webnfc/
  index.html          # Main app (~2800 lines, all-in-one)
  sw.js               # Service Worker (version-synced cache)
  manifest.json       # PWA manifest
  server.py           # Optional Python HTTPS server
  decrypt.html        # Standalone decryption utility
  icon-192.png        # App icon (192x192)
  icon-512.png        # App icon (512x512)
  LICENSE             # Custom non-commercial license
  deploy/             # GitHub Pages deployment (mirrors main files)
    index.html
    sw.js
    manifest.json
    icon-*.png
    .nojekyll
    .well-known/
  docs/               # Specification documents
```

---

## 3. Data Model

### 3.1 Core Entities

```
Course {
  title: string,                    // e.g., "PBS1000 Sample Course"
  professor: string,                // e.g., "Professor Mary Chen"
  professorTimestamps: string[],    // professor check-in timestamps
  students: Student[]
}

Student {
  name: string,                     // student full name
  studentNo: string,                // 8-digit student number or QR content
  last4: string,                    // last 4 digits (auto-derived from studentNo)
  nfcSerial: string,                // encrypted NFC serial (AES-GCM)
  consented: boolean,               // GDPR consent flag
  consentedAt: string,              // consent timestamp
  timestamps: string[]              // attendance records with remarks
}
```

### 3.2 Timestamp Format

```
YYYY-MM-DD HH:MM:SS|<remark>
```

Remark values: `nfc`, `qr code`, `manual`, `password`, `wheel|Excellent`, `wheel|Good`, `wheel|Poor`, `wheel|Refused`

### Professor Global
```
professorGlobal {
  name: string,         // professor name
  nfcSerial: string,    // NFC card serial (or pw:hash if password-only)
  pwHash: string        // SHA-256 password hash (pw: prefixed)
}
```

### 3.3 Storage Keys (localStorage)

| Key | Content |
|-----|---------|
| `nfc_attendance_courses` | JSON object of all courses keyed by title |
| `nfc_attendance_settings` | User preferences (announce, greeting, etc.) |
| `nfc_professor` | Global professor NFC serial + name |
| `nfc_enc_key` | Base64-encoded AES-GCM encryption key |

### 3.4 Settings Object

```javascript
{
  announce: boolean,    // Speech synthesis for names
  greeting: boolean,    // Welcome greeting on open
  encourage: boolean,   // Encouragement messages
  forceOffline: boolean,// Force offline mode
  attendCode: boolean,  // Show Attend Code button (default true on iOS)
  qrScan: boolean       // Show Attend QR button
}
```

---

## 4. Security Architecture

### 4.1 Encryption

- **Algorithm:** AES-GCM (256-bit key)
- **Key generation:** `crypto.getRandomValues(new Uint8Array(32))` on first use
- **Key storage:** Base64 in localStorage under `nfc_enc_key`
- **Scope:** NFC serial numbers encrypted before writing to localStorage
- **Functions:** `encryptSerial(serial)` / `decryptSerial(encrypted)`

### 4.2 Professor Authentication

- **Dual registration:** Professor can register both NFC staff card and password
- **Storage:** `professorGlobal.nfcSerial` for NFC card, `professorGlobal.pwHash` for SHA-256 hashed password
- **Gate function:** `requireAuth(actionName, callback)` wraps all sensitive operations; auth modal dynamically shows only registered methods
- **Helper:** `professorHasAuth()` returns true if either method is registered
- **Protected operations:** Manual attendance, re-registration, data export, clear data, delete course, backup, restore

### 4.3 Content Security Policy

```
default-src 'self';
script-src 'self' 'unsafe-inline';
style-src 'self' 'unsafe-inline';
img-src 'self' data: blob:;
connect-src 'self';
manifest-src 'self';
media-src 'self' blob:;
```

### 4.4 Integrity Check

- Obfuscated copyright/author verification on page load
- Tamper detection renders app unusable if attribution removed

### 4.5 Server Security (server.py)

- HTTPS-only with TLS 1.2+ (HTTP:8090 redirects to HTTPS:8443)
- Whitelisted file serving (only specific static files)
- Thread-safe CSV data access with locks
- Clear password via environment variable or auto-generated

---

## 5. Service Worker Strategy

### 5.1 Cache Versioning

```javascript
const CACHE_NAME = 'smart-attendance-v8.4';
```

Cache name must be bumped with every version change to invalidate stale assets.

### 5.2 Caching Strategy

- **Install:** Pre-cache all static assets (`/`, `/index.html`, `/manifest.json`, `/sw.js`, icons)
- **Activate:** Delete all caches that don't match current `CACHE_NAME`
- **Fetch:** Cache-first for assets; network-first for API calls with cache fallback

---

## 6. Platform Considerations

### 6.1 Android (Primary Platform)

- Full Web NFC support (Chrome 89+)
- BarcodeDetector API for QR scanning
- PWA install via Chrome menu

### 6.2 iOS (Fallback)

- No Web NFC — NFC buttons hidden, defaults to Attend Code mode
- Camera-based QR scanning may have PWA permission issues
- PWA install via Safari "Add to Home Screen"

---

## 7. Deployment Architecture

### 7.1 GitHub Pages (Production)

- Source code on `master` branch
- Deployed files mirrored to `deploy/` directory on `main` branch
- `main` branch served via GitHub Pages
- `.nojekyll` file for static serving

### 7.2 Self-Hosted (Optional)

```bash
python3 server.py
# HTTP:8090 -> HTTPS:8443
# Requires SSL certs in ../certs/
```

---

## 8. Version History

| Version | Date | Changes |
|---------|------|---------|
| v4.3 | 2026 | Initial NFC/QR attendance system |
| v7.5 | 2026 | Double confirm before restore |
| v7.6 | 2026 | NFC mismatch re-register feature (3-strike) |
| v7.7 | 2026-03-30 | Wheel of Fortune attended-only, response recording, group count |
| v7.8 | 2026-04-01 | QR button on student list in NFC attend mode |
| v7.9 | 2026-04-02 | Share CSV via Web Share API; removed Blackboard/Bluetooth |
| v8.0 | 2026-04-02 | Dual professor auth (NFC + password); sample course rename |
| v8.1 | 2026-04-02 | Fix Share CSV Gmail attachment MIME type |
| v8.2 | 2026-04-02 | Fix Share CSV using text/csv for Android Chrome compatibility |
| v8.3 | 2026-04-02 | Restructured CSV export with per-date columns |
| v8.4 | 2026-04-03 | 8-digit student numbers, updated help and docs |
