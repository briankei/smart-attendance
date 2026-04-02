# User & Unit Testing Specification

**Project:** NFC/QR Code Smart Student Attendance PWA
**Version:** v8.3
**Last Updated:** 2026-04-02
**Author:** BrianKei (cwk)

---

## 1. Testing Overview

### 1.1 Testing Approach

All testing is manual due to the zero-build, single-file architecture. Tests require physical devices with NFC capability and camera access.

### 1.2 Test Environment

| Environment | Details |
|------------|---------|
| Primary Device | Android 10+ phone with NFC |
| Browser | Chrome 89+ |
| Protocol | HTTPS (required for Web NFC) |
| Secondary Device | iOS (Safari, Code/QR mode only) |
| NFC Cards | Any NDEF-compatible NFC card/tag |

---

## 2. Test Cases — Professor Setup

### TC-001: First Launch — NFC Setup

| Field | Value |
|-------|-------|
| Precondition | Fresh install, no localStorage data |
| Steps | 1. Open app<br>2. Welcome modal appears<br>3. Tap "Secure with Staff Card (NFC)"<br>4. Scan professor NFC card |
| Expected | Professor NFC serial saved; welcome modal closes; app ready for use |

### TC-002: First Launch — Password Setup

| Field | Value |
|-------|-------|
| Precondition | Fresh install |
| Steps | 1. Open app<br>2. Tap "Secure with Password"<br>3. Enter password, confirm |
| Expected | Password hash saved; app ready |

### TC-003: Professor Authentication — NFC

| Field | Value |
|-------|-------|
| Precondition | Professor NFC registered |
| Steps | 1. Trigger protected action (e.g., Manual attendance)<br>2. Auth modal appears<br>3. Tap "Scan NFC"<br>4. Scan professor card |
| Expected | Action proceeds after successful scan |

### TC-004: Professor Authentication — Password

| Field | Value |
|-------|-------|
| Precondition | Professor password set |
| Steps | 1. Trigger protected action<br>2. Tap "Password"<br>3. Enter correct password |
| Expected | Action proceeds |

---

## 3. Test Cases — Course Management

### TC-010: Upload Student List

| Field | Value |
|-------|-------|
| Precondition | App ready, no courses |
| Steps | 1. Open menu<br>2. Tap Upload Student List<br>3. Select text file with format:<br>`PBS1000 Sample Course`<br>`Professor Chen`<br>`Peter Adoby,1234`<br>`David Lim,9876` |
| Expected | Course created; students listed; summary shows "0 / 2 students attended" |

### TC-011: Upload Second Course

| Field | Value |
|-------|-------|
| Precondition | One course loaded |
| Steps | 1. Upload another text file with different course name |
| Expected | Second course added; can switch between courses in menu |

### TC-012: Delete Course

| Field | Value |
|-------|-------|
| Precondition | Course loaded |
| Steps | 1. Open menu<br>2. Tap delete on course<br>3. Confirm<br>4. Professor auth |
| Expected | Course removed; switches to remaining course or empty state |

### TC-013: Re-upload Same Course (Merge)

| Field | Value |
|-------|-------|
| Precondition | Course with attendance data exists |
| Steps | 1. Upload file with same course name but updated student list |
| Expected | Student list updated; existing attendance data preserved for matching students |

---

## 4. Test Cases — Attend NFC Mode

### TC-020: NFC Attendance — Registered Student

| Field | Value |
|-------|-------|
| Precondition | Course loaded, student NFC registered, Attend NFC mode active |
| Steps | 1. Student taps registered NFC card |
| Expected | Beep; name announced; green highlight; check mark; timestamp with "nfc" remark |

### TC-021: NFC Attendance — Unregistered Card

| Field | Value |
|-------|-------|
| Precondition | Attend NFC mode, card not registered to any student |
| Steps | 1. Tap unknown NFC card |
| Expected | Error status; boo sound; no attendance recorded |

### TC-022: NFC Attendance — Duplicate Same Day

| Field | Value |
|-------|-------|
| Precondition | Student already attended today via NFC |
| Steps | 1. Student taps NFC card again |
| Expected | Status shows already attended; no duplicate timestamp |

### TC-023: NFC Mismatch — 3-Strike Re-register

| Field | Value |
|-------|-------|
| Precondition | Student registered with card A; scanning card B |
| Steps | 1. Scan wrong card 3 times for same student |
| Expected | After 3rd mismatch, re-register prompt appears; professor auth required |

### TC-024: QR Button in NFC Mode

| Field | Value |
|-------|-------|
| Precondition | Attend NFC mode active; student not yet attended today |
| Steps | 1. Tap green "QR" button on student row<br>2. Camera opens<br>3. Scan QR code<br>4. Select correct last 4 digits |
| Expected | Attendance recorded with "qr code" remark; app returns to NFC scan mode |

### TC-025: QR Button — Cancel

| Field | Value |
|-------|-------|
| Precondition | Attend NFC mode |
| Steps | 1. Tap QR button<br>2. Close camera/cancel verification |
| Expected | App returns to NFC scan mode; no attendance recorded |

### TC-026: QR Button — Not Shown for Attended Students

| Field | Value |
|-------|-------|
| Precondition | Student already attended today |
| Steps | 1. Check student row |
| Expected | QR button not displayed for attended students |

---

## 5. Test Cases — Attend Code Mode

### TC-030: Code Verification — Correct

| Field | Value |
|-------|-------|
| Precondition | Attend Code mode; student has last4 set |
| Steps | 1. Tap student name<br>2. Select correct last 4 digits from 3 choices |
| Expected | Attendance recorded with "qr code" remark; beep; name announced |

### TC-031: Code Verification — Wrong

| Field | Value |
|-------|-------|
| Precondition | Attend Code mode |
| Steps | 1. Tap student name<br>2. Select wrong choice |
| Expected | Boo sound; error status; no attendance recorded |

---

## 6. Test Cases — Attend QR Mode

### TC-040: Full QR Flow

| Field | Value |
|-------|-------|
| Precondition | Attend QR mode active |
| Steps | 1. Tap student name<br>2. Camera opens<br>3. Scan student QR code<br>4. QR content saved<br>5. Select correct last 4 digits |
| Expected | Attendance recorded; QR content stored in studentNo; stays in QR mode |

### TC-041: Camera Denied

| Field | Value |
|-------|-------|
| Steps | 1. Tap student name in QR mode<br>2. Deny camera permission |
| Expected | Error: "Camera access denied"; no crash |

---

## 7. Test Cases — Manual Attendance

### TC-050: Manual Attendance

| Field | Value |
|-------|-------|
| Steps | 1. Tap "Manual" on student row<br>2. Confirm attendance<br>3. Professor auth (NFC or password) |
| Expected | Attendance recorded with "manual" remark |

---

## 8. Test Cases — Student Registration

### TC-060: NFC Registration with Consent

| Field | Value |
|-------|-------|
| Precondition | Student not registered |
| Steps | 1. Tap student name in NFC mode<br>2. Consent modal appears<br>3. Student agrees<br>4. Scan NFC card |
| Expected | NFC serial encrypted and saved; consent flag and timestamp set |

### TC-061: QR Registration

| Field | Value |
|-------|-------|
| Steps | 1. Tap student name<br>2. Choose "QR Code" registration<br>3. Scan QR code |
| Expected | QR content saved as identifier |

### TC-062: Re-registration

| Field | Value |
|-------|-------|
| Precondition | Student already registered |
| Steps | 1. Tap "Re-reg" button<br>2. Professor auth<br>3. Register new card/QR |
| Expected | Old registration replaced; new serial saved |

---

## 9. Test Cases — Games

### TC-070: Wheel of Fortune — Attended Only

| Field | Value |
|-------|-------|
| Precondition | 3 students attended, 2 absent |
| Steps | 1. Open Games > Wheel of Fortune<br>2. Tap Play |
| Expected | Wheel only picks from 3 attended students; spinning animation; winner announced |

### TC-071: Wheel of Fortune — No Attended Students

| Field | Value |
|-------|-------|
| Precondition | No students attended today |
| Steps | 1. Open Wheel of Fortune<br>2. Tap Play |
| Expected | Shows "No attended students"; no spin |

### TC-072: Wheel of Fortune — Record Response

| Field | Value |
|-------|-------|
| Precondition | Wheel picked a winner |
| Steps | 1. Tap "Good" response button |
| Expected | Timestamp with "wheel\|Good" saved; display shows "Name — Good"; beep |

### TC-073: Random Groups — By Group Count

| Field | Value |
|-------|-------|
| Precondition | 12 students loaded |
| Steps | 1. Open Random Groups<br>2. Set "No. of groups" to 4<br>3. Tap Shuffle |
| Expected | 4 groups displayed with ~3 students each, evenly distributed |

### TC-074: Random Groups — By Group Size

| Field | Value |
|-------|-------|
| Precondition | 12 students loaded |
| Steps | 1. Set "No. of groups" to Auto<br>2. Set "Group size" to 3<br>3. Tap Shuffle |
| Expected | 4 groups of 3 students each |

---

## 10. Test Cases — Data Management

### TC-080: Export CSV

| Field | Value |
|-------|-------|
| Steps | 1. Open menu<br>2. Tap Export Attendance<br>3. Professor auth |
| Expected | Encrypted CSV file downloaded |

### TC-081: Export Blackboard Format

| Field | Value |
|-------|-------|
| Steps | 1. Open menu<br>2. Tap Blackboard Export<br>3. Configure column name<br>4. Download |
| Expected | Blackboard-compatible file downloaded |

### TC-082: Backup Data

| Field | Value |
|-------|-------|
| Steps | 1. Open menu<br>2. Tap Backup<br>3. Enter passphrase |
| Expected | Encrypted JSON backup file downloaded |

### TC-083: Restore Data

| Field | Value |
|-------|-------|
| Precondition | Have backup file |
| Steps | 1. Open menu<br>2. Tap Restore<br>3. Select backup file<br>4. Enter passphrase |
| Expected | All courses and data restored; double confirmation before overwrite |

### TC-084: Clear All Data

| Field | Value |
|-------|-------|
| Steps | 1. Open menu<br>2. Tap Clear Data<br>3. Professor auth<br>4. Confirm |
| Expected | All localStorage cleared; app returns to welcome screen |

---

## 11. Test Cases — Offline / PWA

### TC-090: Offline Operation

| Field | Value |
|-------|-------|
| Steps | 1. Install PWA<br>2. Turn off network<br>3. Open app<br>4. Perform NFC attendance |
| Expected | App loads from cache; full functionality; data saved to localStorage |

### TC-091: Service Worker Update

| Field | Value |
|-------|-------|
| Precondition | App installed with old version |
| Steps | 1. Deploy new version with bumped cache name<br>2. Open app online |
| Expected | New service worker activates; old cache deleted; new assets cached |

---

## 12. Test Cases — Find Mode

### TC-100: Find Student by NFC

| Field | Value |
|-------|-------|
| Precondition | Find mode active; students registered |
| Steps | 1. Scan registered NFC card |
| Expected | Student row highlighted; scrolled into view; name displayed in status |

---

## 13. Test Cases — Platform-Specific

### TC-110: iOS Fallback

| Field | Value |
|-------|-------|
| Device | iPhone / iPad with Safari |
| Steps | 1. Open app<br>2. Check mode buttons |
| Expected | NFC and Find buttons hidden; defaults to Attend Code mode |

### TC-111: Android Full Features

| Field | Value |
|-------|-------|
| Device | Android 10+ with Chrome |
| Steps | 1. Open app<br>2. Check all mode buttons visible |
| Expected | Attend NFC, Attend Code, Attend QR, Find all available |
