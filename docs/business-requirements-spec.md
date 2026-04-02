# Business Requirements Specification

**Project:** NFC/QR Code Smart Student Attendance PWA
**Version:** v7.8
**Last Updated:** 2026-04-02
**Author:** BrianKei (cwk)

---

## 1. Business Objectives

### 1.1 Purpose

Provide a fast, reliable, and privacy-respecting classroom attendance tracking system that works offline on mobile devices using NFC cards and QR codes.

### 1.2 Target Users

- **Primary:** University professors/lecturers
- **Secondary:** Students (passive NFC tap or active QR/Code verification)

### 1.3 Key Business Goals

- Eliminate manual paper-based attendance roll calls
- Record attendance in under 2 seconds per student via NFC
- Support classrooms without internet connectivity (offline-first)
- Comply with GDPR/privacy requirements via explicit consent and encryption
- Support multiple courses per professor on a single device

---

## 2. Functional Requirements

### 2.1 Professor Setup & Authentication

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-001 | Professor can secure the app with NFC staff card or password on first launch | Must |
| BR-002 | Sensitive operations require professor re-authentication (NFC or password) | Must |
| BR-003 | Professor NFC card is globally authorized across all courses | Must |

### 2.2 Course Management

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-010 | Upload student list from text file (name + last 4 digits of student number) | Must |
| BR-011 | Support extended format (name, full student no., NFC serial, consent date) | Should |
| BR-012 | Support multiple courses with tab-based switching | Must |
| BR-013 | Delete a course (with professor authorization) | Must |
| BR-014 | Merge/update student list when re-uploading for same course | Should |

### 2.3 Attendance Modes

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-020 | **Attend NFC** — Continuous NFC card scanning for attendance | Must |
| BR-021 | **Attend Code** — Student taps name, verifies with last-4-digit selection | Must |
| BR-022 | **Attend QR** — Student taps name, scans QR code via camera, then last-4-digit verification | Must |
| BR-023 | **QR from NFC mode** — QR button per student during NFC mode; follows Attend QR flow then returns to NFC mode | Must |
| BR-024 | **Find** — Scan NFC card to locate/highlight student in list | Must |
| BR-025 | **Manual** — Professor-authorized manual attendance with double confirmation | Must |
| BR-026 | Attendance timestamps include method remark (nfc/qr code/manual) | Must |
| BR-027 | Prevent duplicate attendance for same student on same day (within same mode) | Should |

### 2.4 Student Registration

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-030 | Register student NFC card with explicit consent prompt | Must |
| BR-031 | Register student via QR code as alternative to NFC | Must |
| BR-032 | Re-register NFC/QR with professor authorization | Must |
| BR-033 | NFC mismatch detection — 3 consecutive mismatches trigger re-register prompt | Must |
| BR-034 | GDPR consent tracking with timestamp | Must |

### 2.5 Data Management

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-040 | Export attendance as encrypted CSV | Must |
| BR-041 | Export in Blackboard LMS format | Should |
| BR-042 | Backup/restore all app data with passphrase encryption | Must |
| BR-043 | Transfer data via Bluetooth file sharing | Should |
| BR-044 | Clear all data with professor authorization | Must |
| BR-045 | View attendance summary (present/total count per day) | Must |
| BR-046 | View individual student attendance history (Log button) | Must |

### 2.6 Games / Classroom Engagement

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-050 | **Wheel of Fortune** — Random pick from attended students only | Should |
| BR-051 | Record student response after wheel pick (Refused/Poor/Good/Excellent) | Should |
| BR-052 | **Random Groups** — Shuffle students into groups by size or by number of groups (3-12) | Should |
| BR-053 | **Random Order** — Shuffle student presentation order | Should |
| BR-054 | **Countdown Timer** — Classroom timer | Could |

### 2.7 User Experience

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-060 | Audio feedback — beep on successful scan, boo on failure | Must |
| BR-061 | Speech synthesis — announce student name on attendance | Should |
| BR-062 | Encouragement messages (toggleable) | Could |
| BR-063 | Visual highlight on attendance event (green flash) | Must |
| BR-064 | Auto-scroll to student on attendance event | Should |
| BR-065 | Dark theme UI optimized for mobile | Must |
| BR-066 | Install as PWA for offline use (Android + iOS) | Must |

### 2.8 Settings

| ID | Requirement | Priority |
|----|-------------|----------|
| BR-070 | Toggle audio announcements | Must |
| BR-071 | Toggle greeting on app open | Should |
| BR-072 | Toggle encouragement messages | Could |
| BR-073 | Toggle Attend Code mode visibility | Must |
| BR-074 | Toggle Attend QR mode visibility | Must |
| BR-075 | Install PWA from menu | Should |

---

## 3. Non-Functional Requirements

### 3.1 Performance

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | NFC scan to attendance confirmation | < 2 seconds |
| NFR-002 | App load time (cached) | < 1 second |
| NFR-003 | Support class sizes | Up to 200 students |

### 3.2 Security

| ID | Requirement |
|----|-------------|
| NFR-010 | NFC serials encrypted at rest (AES-GCM) |
| NFR-011 | HTTPS required for NFC API access |
| NFR-012 | Professor authentication for destructive operations |
| NFR-013 | Content Security Policy enforced |
| NFR-014 | No external network calls in normal operation |

### 3.3 Compatibility

| ID | Requirement |
|----|-------------|
| NFR-020 | Android: Chrome 89+ on Android 10+ |
| NFR-021 | iOS: Safari (Attend Code mode only, no NFC) |
| NFR-022 | Offline operation after initial load |

### 3.4 Privacy & Compliance

| ID | Requirement |
|----|-------------|
| NFR-030 | Explicit student consent before NFC registration |
| NFR-031 | Consent timestamp recorded |
| NFR-032 | All data stored locally on professor's device only |
| NFR-033 | Encrypted export for data portability |

---

## 4. User Stories

### Professor Stories

- **US-001:** As a professor, I want to scan NFC cards continuously so that students can tap and go without waiting.
- **US-002:** As a professor, I want to switch between NFC, Code, and QR modes so I can handle different classroom setups.
- **US-003:** As a professor, I want to manage multiple courses so I don't need separate devices per class.
- **US-004:** As a professor, I want to export attendance to Blackboard so I can sync with the LMS.
- **US-005:** As a professor, I want to use the Wheel of Fortune to randomly pick a student for questions, and record their response quality.
- **US-006:** As a professor, I want a QR button on each student row during NFC mode so students without NFC cards can still check in via QR.

### Student Stories

- **US-010:** As a student, I want to tap my NFC card and hear my name announced so I know attendance was recorded.
- **US-011:** As a student, I want to verify my identity with last 4 digits so I can attend via Code mode.
- **US-012:** As a student, I want to scan my QR code during NFC mode if I forgot my NFC card.

---

## 5. Acceptance Criteria Summary

| Feature | Criteria |
|---------|----------|
| NFC Attendance | Scan card -> beep -> name announced -> green highlight -> timestamp saved with "nfc" remark |
| QR Attendance | Tap name -> camera opens -> scan QR -> select last 4 digits -> timestamp saved with "qr code" remark |
| QR from NFC mode | Tap QR button -> full QR flow -> returns to NFC scan mode after completion |
| Manual Attendance | Tap Manual -> confirm -> professor auth -> timestamp saved with "manual" remark |
| Wheel of Fortune | Only picks from today's attended students; shows response buttons after pick |
| Random Groups | Supports group-by-size (2-5) and group-by-count (3-12) |
| Data Export | Encrypted CSV download; Blackboard format export |
| Backup/Restore | Passphrase-encrypted JSON; full app state preserved |
