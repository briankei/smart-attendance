# User Manual

**NFC/QR Code Smart Student Attendance PWA**
**Version:** v8.3
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

On first launch, a Welcome screen appears. Choose a security method:

- **Secure with Staff Card (NFC):** Scan your professor NFC/staff card.
- **Secure with Password:** Enter and confirm a password (min 6 characters).

After registering the first method, the app will prompt you to also set up the other method as a backup. **Both NFC card and password can be registered** — either one can be used to authorize sensitive operations.

---

## 2. Uploading a Student List

### 2.1 File Format

Create a plain text file (.txt) with this format:

```
PBS 1000 Sample Course
Professor Mary Chen
Peter Adoby,24011234
Peter Bai,24025432
David Lim,24039876
```

- **Line 1:** Course name
- **Line 2:** Professor name
- **Line 3+:** Student name, student number (comma or tab separated). Last 4 digits are used for Attend Code verification.

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

**QR Fallback:** Each unattended student row shows a green **QR** button. Tap it to use the QR scanning process (camera scan + last-4-digit verification), then the app returns to NFC mode automatically.

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

## 4. Professor Authentication

The app supports **dual authentication** — professors can register both an NFC staff card and a password.

### 4.1 Registering Both Methods

- On first launch, after choosing one method, the app prompts to also register the other
- You can also register/change methods from the menu at any time

### 4.2 Authorizing Operations

When a protected action requires authorization (e.g., manual attendance, data export, clear data), the auth modal shows only the methods you have registered:

- **Scan NFC** — tap your registered staff card
- **Password** — enter your professor password

### 4.3 Professor Status Display

The menu shows your registered methods:
- `Professor Name: NFC | Password` — both registered
- `Professor Name: Password` — password only
- `Professor Name: NFC` — NFC only

---

## 5. Student Registration

### 5.1 NFC Registration

1. In Attend NFC mode, tap an unregistered student's name
2. A consent prompt appears — student must agree
3. Student taps their NFC card
4. Card serial is encrypted and saved

### 5.2 QR Registration

1. Tap an unregistered student's name
2. Choose **"QR Code"** registration method
3. Scan the student's QR code

### 5.3 Re-registration

If a student needs a new card:

1. Tap the **Re-reg** button on their row
2. Professor authorization required
3. Register the new NFC card or QR code

### 5.4 NFC Mismatch Detection

If a student scans the wrong card 3 times in a row, the app prompts to re-register. This requires professor authorization.

---

## 6. Student List Display

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

## 7. Games — Classroom Engagement

Access via the **Games** button (orange).

### 7.1 Wheel of Fortune

- Randomly picks from **today's attended students only**
- Spinning animation with bee-boo sound effects
- After picking a student, **response recording** buttons appear:
  - **Refused** (red) — student declined to answer
  - **Poor** (amber) — poor response
  - **Good** (green) — good response
  - **Excellent** (purple) — excellent response
- Response is saved as a timestamp (visible in student's Log and CSV export)

### 7.2 Random Groups

- Shuffle students into groups
- **Group size:** Select 2, 3, 4, or 5 students per group
- **No. of groups:** Select 3 to 12 groups, or "Auto" to use group size
- Tap **Shuffle** to regenerate

### 7.3 Random Order

- Shuffles all students into a random presentation order
- Tap **Shuffle** to regenerate

### 7.4 Countdown Timer

- Classroom timer for activities

---

## 8. Data Management

All accessible from the hamburger menu.

### 8.1 Export CSV

Downloads a structured CSV file to the device.

1. Open menu > **Export CSV**
2. CSV file downloads automatically

**CSV format:** Each date gets its own column. Each cell contains comma-delimited activity entries for that student on that date.

Example output:
```
Course,"PBS 1000 Sample Course"
Professor,"Professor Chen"

Name,Student No,Serial (encrypted),Consented,Consent Date,2026-04-01,2026-04-02
"Peter Adoby","24011234","...","Yes","2026-03-15","14:30:00 nfc","09:15:00 nfc; 10:20:00 wheel Good"
"David Lim","24039876","...","Yes","2026-03-15",,"09:18:00 qr code"
```

Each activity entry contains: `time type detail` (e.g., `14:30:00 nfc`, `10:20:00 wheel Good`, `09:00:00 manual`). Multiple activities on the same day are separated by semicolons.

### 8.2 Share CSV

Shares the CSV file directly to other apps (Gmail, WhatsApp, Google Drive, etc.) via the native Android/iOS share sheet.

1. Open menu > **Share CSV**
2. The share sheet appears — choose the app to send to
3. The CSV file is attached automatically

**Note:** The shared CSV omits encrypted NFC serials for privacy.

### 8.3 Backup

1. Open menu > **Backup All Data**
2. Professor authorization required
3. Enter a passphrase (remember it!)
4. Encrypted JSON file downloads with all courses and settings

### 8.4 Restore

1. Open menu > **Restore Backup**
2. Select backup file
3. Enter the passphrase used during backup
4. Confirm twice (this replaces all current data)

**Important:** Export your current data before restoring, as restore overwrites everything.

### 8.5 Clear All Data

1. Open menu > **Clear All Data**
2. Professor authorization required
3. Confirm to delete all courses, settings, and encryption keys
4. App returns to welcome screen

### 8.6 App Reset

Complete factory reset — removes all data including professor registration.

1. Open menu > **App Reset**
2. Double confirmation required
3. Professor authorization required
4. All localStorage cleared; app returns to welcome screen

---

## 9. Settings

Accessible from the hamburger menu, under **Settings**.

| Setting | Description | Default |
|---------|-------------|---------|
| Audio Announcements | Speak student name on attendance | On |
| Greeting | Show welcome message on app open | On |
| Encouragement | Show motivational messages | Off |
| Attend Code | Show Attend Code mode button | On (iOS), Off (Android) |
| QR Scan | Show Attend QR mode button | On |

---

## 10. Tips for Professors

- **Before class:** Upload student list, ensure app is installed and NFC mode is active
- **During class:** Place device at entrance or pass around for students to tap
- **QR fallback:** Students without NFC cards can use the QR button on their row
- **After class:** Use Wheel of Fortune to pick students for review questions
- **End of term:** Share or export attendance CSV via Gmail or other apps
- **Device transfer:** Use Backup/Restore to move data between devices
- **Multiple sections:** Upload separate student lists per course section
- **Security:** Register both NFC card and password for backup access

---

## 11. Troubleshooting

| Issue | Solution |
|-------|----------|
| "Web NFC not supported" | Use Chrome 89+ on Android 10+. Ensure HTTPS. |
| NFC not reading | Ensure NFC is enabled in device settings. Hold card steady for 1-2 seconds. |
| Camera not opening | Grant camera permission in browser settings |
| App not loading offline | Reinstall PWA; ensure service worker is registered |
| "No attended students" in Wheel | Students must have attended today before using Wheel of Fortune |
| Share CSV fails | Ensure Chrome is up to date; try Export CSV as fallback |
| Forgot password | If NFC card is registered, use it to authorize. Otherwise, clear browser data and re-setup (data will be lost). |
| Student list not parsing | Check file format: plain text, one student per line, comma-separated |
