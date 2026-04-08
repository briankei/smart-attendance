/**
 * BLE Attendance Module for Smart Attendance App
 * Provides BLE advertising (professor) and scanning (student) via Capacitor BLE plugin
 */

const BLE_SERVICE_UUID = '12345678-1234-1234-1234-123456789abc';
const BLE_CHAR_UUID = '12345678-1234-1234-1234-123456789abd';
const BLE_DEVICE_NAME = 'SmartAttendance';

// Check if running in Capacitor native context
function isNative() {
    return window.Capacitor && window.Capacitor.isNativePlatform();
}

// ---- Professor Side: BLE Peripheral (Advertise + Receive) ----

let bleAdvertising = false;
let connectedStudents = new Set();

async function startBLEAdvertising(courseTitle) {
    if (!isNative()) {
        console.log('BLE advertising requires native app');
        return false;
    }

    try {
        const { BleClient } = await import('@capacitor-community/bluetooth-le');
        await BleClient.initialize({ androidNeverForLocation: true });

        // Start advertising as a peripheral
        await BleClient.startAdvertising({
            localName: BLE_DEVICE_NAME,
            serviceUuids: [BLE_SERVICE_UUID],
            manufacturerData: [{
                companyIdentifier: 0xFFFF,
                data: new TextEncoder().encode(courseTitle.slice(0, 20))
            }]
        });

        bleAdvertising = true;
        console.log('BLE advertising started for:', courseTitle);
        return true;
    } catch (e) {
        console.error('BLE advertising failed:', e);
        return false;
    }
}

async function stopBLEAdvertising() {
    if (!isNative() || !bleAdvertising) return;
    try {
        const { BleClient } = await import('@capacitor-community/bluetooth-le');
        await BleClient.stopAdvertising();
        bleAdvertising = false;
    } catch (e) {
        console.error('Stop advertising failed:', e);
    }
}

// ---- Student Side: Scan + Connect + Send Student Number ----

async function scanForProfessor() {
    if (!isNative()) {
        console.log('BLE scanning requires native app');
        return null;
    }

    try {
        const { BleClient } = await import('@capacitor-community/bluetooth-le');
        await BleClient.initialize({ androidNeverForLocation: true });

        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                BleClient.stopLEScan();
                reject(new Error('No professor device found'));
            }, 10000);

            BleClient.requestLEScan(
                { services: [BLE_SERVICE_UUID] },
                (result) => {
                    clearTimeout(timeout);
                    BleClient.stopLEScan();
                    resolve(result.device);
                }
            );
        });
    } catch (e) {
        console.error('BLE scan failed:', e);
        return null;
    }
}

async function sendStudentNumber(deviceId, studentNo) {
    if (!isNative()) return false;

    try {
        const { BleClient } = await import('@capacitor-community/bluetooth-le');
        await BleClient.connect(deviceId);

        const data = new TextEncoder().encode(studentNo);
        await BleClient.write(deviceId, BLE_SERVICE_UUID, BLE_CHAR_UUID, data);

        await BleClient.disconnect(deviceId);
        return true;
    } catch (e) {
        console.error('BLE send failed:', e);
        return false;
    }
}

// ---- QR Code Generation for Student Check-in URL ----

function generateCheckInQR(serverUrl, courseId, sessionToken) {
    const url = `${serverUrl}/checkin.html?course=${encodeURIComponent(courseId)}&token=${sessionToken}`;
    return url;
}

function generateSessionToken() {
    const arr = new Uint8Array(16);
    crypto.getRandomValues(arr);
    return Array.from(arr).map(b => b.toString(16).padStart(2, '0')).join('');
}

// ---- Rotating Session Token ----
let rotatingToken = null;
let rotateInterval = null;

function startRotatingToken(intervalMs = 30000) {
    rotatingToken = generateSessionToken();
    rotateInterval = setInterval(() => {
        rotatingToken = generateSessionToken();
        // Dispatch event so UI can update QR code
        window.dispatchEvent(new CustomEvent('tokenRotated', { detail: { token: rotatingToken } }));
    }, intervalMs);
    return rotatingToken;
}

function stopRotatingToken() {
    if (rotateInterval) {
        clearInterval(rotateInterval);
        rotateInterval = null;
    }
    rotatingToken = null;
}

function getCurrentToken() {
    return rotatingToken;
}

// Export for use in main app
window.BLEAttendance = {
    isNative,
    startBLEAdvertising,
    stopBLEAdvertising,
    scanForProfessor,
    sendStudentNumber,
    generateCheckInQR,
    generateSessionToken,
    startRotatingToken,
    stopRotatingToken,
    getCurrentToken,
    BLE_SERVICE_UUID,
    BLE_DEVICE_NAME
};
