# User Manual

**NFC/QR Code Smart Student Attendance PWA**
**Version:** v7.8
**Last Updated:** 2026-04-02

---

## 1. Getting Started

### 1.1 Requirements

- **Android:** Chrome 89+ on Android 10+ (full NFC support)
- **iOS:** Safari (Code/QR mode only — no NFC)
- **Connection:** HTTPS required for initial load and NFC access
- **NFC Cards:** Any NDEF-compatible NFC card or tag

### 1.2 Installation

**Android:**
1. Open the app URL in Chrome
2. Open the hamburger menu (top-left) and tap **"Install App (PWA)"**
3. Or tap Chrome's menu (...) > **"Add to Home screen"**

**iPhone / iPad:**
1. Open the app URL in **Safari** (not Chrome)
2. Tap the **Share** button (square with arrow)
3. Choose **"Add to Home Screen"**

After installing, open the app from your home screen. It works fully offline.

### 1.3 First-Time Setup

On first launch, a Welcome screen appears. Choose one security method:

- **Secure with Staff Card (NFC):** Scan your professor NFC/staff card. This card will be used to authorize sensitive operations.
- **Secure with Password:** Enter and confirm a password.

---

## 2. Uploading a Student List

### 2.1 File Format

Create a plain text file (.txt) with this format:

```
PBS1000 Sample Course
Professor Mary Chen
Peter Adoby,1234
Peter Bai,5432
David Lim,9876
```

- **Line 1:** Course name
- **Line 2:** Professor name
- **Line 3+:** Student name, last 4 digits of student number (comma or tab separated)

**Extended format** (for importing existing registrations):
```
Name,StudentNo,NFCSerial,ConsentDate
```

### 2.2 How to Upload

1. Tap the hamburger menu (top-left)
2. Tap **Upload Student List**
3. Select your text file
4. Students appear in the list below

### 2.3 Multiple Courses

Upload additional text files with different course names. Switch between courses from the **Courses** section in the menu.

---

## 3. Attendance Modes

### 3.1 Attend NFC (Primary)

Best for: Android devices with NFC capability.

1. Tap the **Attend NFC** button (green)
2. Status bar shows "ATTEND NFC mode — scan NFC cards for attendance"
3. Students tap their NFC cards one by one
4. On successful scan: beep, name announced, green highlight, check mark appears

**QR Fallback:** Each unattended student row shows a green **QR** button. Tap it to use the QR scanning process instead, then the app returns to NFC mode automatically.

### 3.2 Attend Code

Best for: iOS devices or when NFC is unavailable.

1. Tap the **Attend Code** button (pink)
2. Student taps their name in the list
3. Three 4-digit codes appear — student selects their last 4 digits of student number
4. Correct selection records attendance

### 3.3 Attend QR

Best for: QR code-based student ID verification.

1. Tap the **Attend QR** button (magenta)
2. Student taps their name
3. Camera opens to scan their QR code
4. After scanning, last-4-digit verification appears
5. Correct selection records attendance

### 3.4 Find Mode

1. Tap the **Find** button (amber)
2. Scan any registered NFC card
3. The matching student is highlighted and scrolled into view

### 3.5 Manual Attendance

For students who forgot their NFC card and cannot use QR:

1. Tap the **Manual** button (purple) on the student's row
2. Confirm the attendance
3. Authorize with professor NFC card or password

---

## 4. Student Registration

### 4.1 NFC Registration

1. In Attend NFC mode, tap an unregistered student's name
2. A consent prompt appears — student must agree
3. Student taps their NFC card
4. Card serial is encrypted and saved

### 4.2 QR Registration

1. Tap an unregistered student's name
2. Choose **"QR Code"** registration method
3. Scan the student's QR code

### 4.3 Re-registration

If a student needs a new card:

1. Tap the **Re-reg** button on their row
2. Professor authorization required
3. Register the new NFC card or QR code

### 4.4 NFC Mismatch Detection

If a student scans the wrong card 3 times in a row, the app prompts to re-register. This requires professor authorization.

---

## 5. Student List Display

Each student row shows:

| Element | Description |
|---------|-------------|
| Number | Student index in list |
| Name + check mark | Student name; green check if attended today |
| **Manual** button | Record manual attendance (purple) |
| **QR** button | QR attendance shortcut in NFC mode (green, only when not yet attended) |
| **Log** button | View attendance history (blue, shown if has records) |
| **Re-reg** button | Re-register NFC/QR (grey, shown if registered) |
| NFC status | Registration status — serial preview or "Tap to Register" |
| Last timestamp | Most recent attendance time and method tag (nfc/qr/manual) |

**Summary bar** at top: shows "Today: X / Y students attended"

---

## 6. Games — Classroom Engagement

Access via the **Games** button (orange).

### 6.1 Wheel of Fortune

- Randomly picks from **today's attended students only**
- Spinning animation with bee-boo sound effects
- After picking a student, **response recording** buttons appear:
  - **Refused** (red) — student declined to answer
  - **Poor** (amber) — poor response
  - **Good** (green) — good response
  - **Excellent** (purple) — excellent response
- Response is saved as a timestamp (visible in student's Log)

### 6.2 Random Groups

- Shuffle students into groups
- **Group size:** Select 2, 3, 4, or 5 students per group
- **No. of groups:** Select 3 to 12 groups, or "Auto" to use group size
- Tap **Shuffle** to regenerate

### 6.3 Random Order

- Shuffles all students into a random presentation order
- Tap **Shuffle** to regenerate

### 6.4 Countdown Timer

- Classroom timer for activities

---

## 7. Data Management

All accessible from the hamburger menu.

### 7.1 Export Attendance (CSV)

1. Open menu > **Export Attendance**
2. Professor authorization required
3. Encrypted CSV file downloads automatically

### 7.2 Blackboard Export

1. Open menu > **Blackboard Export**
2. Set column name (defaults to today's date)
3. Preview and download

### 7.3 Backup

1. Open menu > **Backup Data**
2. Enter a passphrase (remember it!)
3. Encrypted JSON file downloads with all courses and settings

### 7.4 Restore

1. Open menu > **Restore Data**
2. Select backup file
3. Enter the passphrase used during backup
4. Confirm twice (this replaces all current data)

**Important:** Export your current data before restoring, as restore overwrites everything.

### 7.5 Bluetooth Transfer

- Share attendance data files via Bluetooth to another device

### 7.6 Clear All Data

1. Open menu > **Clear Data**
2. Professor authorization required
3. Confirm to delete all courses, settings, and encryption keys
4. App returns to welcome screen

---

## 8. Settings

Accessible from the hamburger menu, under **Settings**.

| Setting | Description | Default |
|---------|-------------|---------|
| Audio Announcements | Speak student name on attendance | On |
| Greeting | Show welcome message on app open | On |
| Encouragement | Show motivational messages | Off |
| Attend Code | Show Attend Code mode button | On (iOS), Off (Android) |
| QR Scan | Show Attend QR mode button | On |

---

## 9. Tips for Professors

- **Before class:** Upload student list, ensure app is installed and NFC mode is active
- **During class:** Place device at entrance or pass around for students to tap
- **QR fallback:** Students without NFC cards can use the QR button on their row
- **After class:** Use Wheel of Fortune to pick students for review questions
- **End of term:** Export all attendance data to Blackboard or CSV
- **Device transfer:** Use Backup/Restore to move data between devices
- **Multiple sections:** Upload separate student lists per course section

---

## 10. Troubleshooting

| Issue | Solution |
|-------|----------|
| "Web NFC not supported" | Use Chrome 89+ on Android 10+. Ensure HTTPS. |
| NFC not reading | Ensure NFC is enabled in device settings. Hold card steady for 1-2 seconds. |
| Camera not opening | Grant camera permission in browser settings |
| App not loading offline | Reinstall PWA; ensure service worker is registered |
| "No attended students" in Wheel | Students must have attended today before using Wheel of Fortune |
| Forgot password | Clear browser data for the app and re-setup (data will be lost) |
| Student list not parsing | Check file format: plain text, one student per line, comma-separated |
