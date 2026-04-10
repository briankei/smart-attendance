package com.briankei.smartattendance;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelUuid;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.content.FileProvider;

import androidx.core.app.ActivityCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@CapacitorPlugin(
    name = "BleAttendance",
    permissions = {
        @Permission(strings = { Manifest.permission.BLUETOOTH_ADVERTISE }, alias = "bluetooth_advertise"),
        @Permission(strings = { Manifest.permission.BLUETOOTH_CONNECT }, alias = "bluetooth_connect"),
        @Permission(strings = { Manifest.permission.BLUETOOTH_SCAN }, alias = "bluetooth_scan"),
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION }, alias = "location")
    }
)
public class BleAttendancePlugin extends Plugin {

    private static final String TAG = "BleAttendance";
    private static final int PERMISSION_REQUEST_CODE = 9001;

    public static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    public static final UUID CHAR_WRITE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd");
    public static final UUID CHAR_READ_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServer gattServer;
    private boolean isAdvertising = false;
    private String courseInfo = "";
    private PluginCall pendingCall = null;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final java.util.Map<String, String> bleResponseMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Integer> deviceRssi = new java.util.concurrent.ConcurrentHashMap<>();
    private BluetoothLeScanner bleScanner;
    private boolean isScanning = false;

    @Override
    public void load() {
        super.load();
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(java.util.Locale.US);
                tts.setSpeechRate(0.9f);
                ttsReady = true;
                Log.d(TAG, "TTS initialized");
            } else {
                Log.e(TAG, "TTS init failed");
            }
        });
    }

    @PluginMethod
    public void startAdvertising(PluginCall call) {
        try {
            // Check and request permissions first
            if (!hasBlePermissions()) {
                pendingCall = call;
                requestPermissions();
                return;
            }

            doStartAdvertising(call);
        } catch (Exception e) {
            Log.e(TAG, "startAdvertising error", e);
            call.reject("BLE error: " + e.getMessage());
        }
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(getActivity(),
                new String[]{
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && pendingCall != null) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                doStartAdvertising(pendingCall);
            } else {
                pendingCall.reject("Bluetooth permissions denied. Please grant permissions in Settings.");
            }
            pendingCall = null;
        }
    }

    private void doStartAdvertising(PluginCall call) {
        try {
            String course = call.getString("course", "SmartAttendance");
            courseInfo = course;

            bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                call.reject("Bluetooth not available on this device");
                return;
            }

            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                call.reject("Bluetooth is not enabled. Please turn on Bluetooth.");
                return;
            }

            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser == null) {
                call.reject("BLE advertising not supported on this device");
                return;
            }

            // Set a recognizable device name for students to find
            try {
                bluetoothAdapter.setName("SmartAttendance");
            } catch (Exception e) {
                Log.w(TAG, "Could not set BLE name", e);
            }

            // Start GATT server first
            startGattServer();

            // Configure advertising
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

            // Main advertise data: service UUID only (must fit in 31 bytes)
            AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

            // Scan response: device name (sent when scanner requests more info)
            AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

            Log.d(TAG, "Starting BLE advertising with name: " + bluetoothAdapter.getName());

            advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback);
            isAdvertising = true;

            JSObject ret = new JSObject();
            ret.put("status", "advertising");
            ret.put("course", course);
            ret.put("serviceUuid", SERVICE_UUID.toString());
            ret.put("ipAddress", getLocalIPAddress());
            call.resolve(ret);

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
            call.reject("Bluetooth permission denied: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "doStartAdvertising error", e);
            call.reject("Failed to start BLE: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopAdvertising(PluginCall call) {
        try {
            stopAll();
            JSObject ret = new JSObject();
            ret.put("status", "stopped");
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Stop error: " + e.getMessage());
        }
    }

    @PluginMethod
    public void isAdvertising(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("advertising", isAdvertising);
        call.resolve(ret);
    }

    @PluginMethod
    public void speak(PluginCall call) {
        String text = call.getString("text", "");
        if (text.isEmpty()) { call.resolve(); return; }
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "attendance_" + System.currentTimeMillis());
            Log.d(TAG, "TTS speaking: " + text);
        } else {
            Log.w(TAG, "TTS not ready");
        }
        call.resolve();
    }

    @PluginMethod
    public void shareFile(PluginCall call) {
        String content = call.getString("content", "");
        String fileName = call.getString("fileName", "file.csv");
        String mimeType = call.getString("mimeType", "text/csv");

        try {
            // Write to cache dir
            java.io.File cacheDir = new java.io.File(getContext().getCacheDir(), "shared");
            cacheDir.mkdirs();
            java.io.File file = new java.io.File(cacheDir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(content);
            writer.close();

            Uri uri = FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            getActivity().startActivity(Intent.createChooser(shareIntent, "Share " + fileName));
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "shareFile error", e);
            call.reject("Share failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void saveFile(PluginCall call) {
        String content = call.getString("content", "");
        String fileName = call.getString("fileName", "file.csv");

        try {
            java.io.File dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(dlDir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(content);
            writer.close();

            JSObject ret = new JSObject();
            ret.put("path", file.getAbsolutePath());
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "saveFile error", e);
            call.reject("Save failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void shareImage(PluginCall call) {
        String base64 = call.getString("base64", "");
        String fileName = call.getString("fileName", "qrcode.png");

        try {
            byte[] imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);

            java.io.File cacheDir = new java.io.File(getContext().getCacheDir(), "shared");
            cacheDir.mkdirs();
            java.io.File file = new java.io.File(cacheDir, fileName);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(imageBytes);
            fos.close();

            Uri uri = FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            getActivity().startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "shareImage error", e);
            call.reject("Share failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setResponse(PluginCall call) {
        String response = call.getString("response", "");
        String deviceAddr = call.getString("deviceAddress", "");
        // Append distance info if available
        Integer rssi = deviceRssi.get(deviceAddr);
        if (rssi != null) {
            double dist = rssiToDistance(rssi);
            response += "|" + distanceLabel(dist);
        }
        if (!deviceAddr.isEmpty()) {
            bleResponseMap.put(deviceAddr, response);
        }
        Log.d(TAG, "BLE response set for " + deviceAddr + ": " + response);
        call.resolve();
    }

    @PluginMethod
    public void getDeviceIP(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("ip", getLocalIPAddress());
        call.resolve(ret);
    }

    private String getLocalIPAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                // Prefer wlan or hotspot interfaces
                String name = intf.getName().toLowerCase();
                if (!intf.isUp() || intf.isLoopback()) continue;
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip != null && ip.indexOf(':') < 0) { // IPv4 only
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getLocalIPAddress error", e);
        }
        return "0.0.0.0";
    }

    private void startGattServer() {
        try {
            if (gattServer != null) {
                gattServer.close();
            }

            gattServer = bluetoothManager.openGattServer(getContext(), gattServerCallback);
            if (gattServer == null) {
                Log.e(TAG, "Failed to open GATT server");
                return;
            }

            BluetoothGattService service = new BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            );

            // Writable characteristic — students write their student number
            BluetoothGattCharacteristic writeChar = new BluetoothGattCharacteristic(
                CHAR_WRITE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            );
            service.addCharacteristic(writeChar);

            // Readable characteristic — course info
            BluetoothGattCharacteristic readChar = new BluetoothGattCharacteristic(
                CHAR_READ_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            );
            readChar.setValue(courseInfo.getBytes(StandardCharsets.UTF_8));
            service.addCharacteristic(readChar);

            gattServer.addService(service);
            Log.d(TAG, "GATT server started with service: " + SERVICE_UUID);
        } catch (SecurityException e) {
            Log.e(TAG, "GATT server SecurityException", e);
        } catch (Exception e) {
            Log.e(TAG, "GATT server error", e);
        }
    }

    private void stopAll() {
        stopRssiScan();
        try {
            if (advertiser != null && isAdvertising) {
                advertiser.stopAdvertising(advertiseCallback);
            }
        } catch (Exception e) {
            Log.w(TAG, "Stop advertising error: " + e.getMessage());
        }
        try {
            if (gattServer != null) {
                gattServer.close();
                gattServer = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Close GATT error: " + e.getMessage());
        }
        isAdvertising = false;
    }

    private void startRssiScan() {
        try {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bleScanner == null) return;
            ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(0)
                .build();
            bleScanner.startScan(null, scanSettings, rssiScanCallback);
            isScanning = true;
            Log.d(TAG, "RSSI scan started");
        } catch (Exception e) {
            Log.w(TAG, "RSSI scan failed", e);
        }
    }

    private void stopRssiScan() {
        if (bleScanner != null && isScanning) {
            try { bleScanner.stopScan(rssiScanCallback); } catch (Exception e) { /* ignore */ }
            isScanning = false;
        }
    }

    private final ScanCallback rssiScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null && result.getDevice() != null) {
                String address = result.getDevice().getAddress();
                int rssi = result.getRssi();
                deviceRssi.put(address, rssi);
            }
        }
    };

    private static double rssiToDistance(int rssi) {
        // Path loss model: distance = 10 ^ ((txPower - rssi) / (10 * n))
        // txPower: RSSI at 1 meter (typically -59 dBm)
        // n: path loss exponent (2.0 for free space, 2.5-3.0 indoors)
        int txPower = -59;
        double n = 2.5;
        return Math.pow(10.0, (txPower - rssi) / (10.0 * n));
    }

    private static String distanceLabel(double meters) {
        if (meters < 1.5) return String.format("%.1fm (very close)", meters);
        if (meters < 5) return String.format("%.1fm (nearby)", meters);
        if (meters < 10) return String.format("%.1fm (medium)", meters);
        return String.format("%.0fm (far)", meters);
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertising started successfully");
            isAdvertising = true;
            startRssiScan();
            JSObject data = new JSObject();
            data.put("event", "advertiseOk");
            notifyListeners("bleEvent", data);
        }

        @Override
        public void onStartFailure(int errorCode) {
            String reason;
            switch (errorCode) {
                case ADVERTISE_FAILED_DATA_TOO_LARGE: reason = "Data too large"; break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS: reason = "Too many advertisers"; break;
                case ADVERTISE_FAILED_ALREADY_STARTED: reason = "Already started"; break;
                case ADVERTISE_FAILED_INTERNAL_ERROR: reason = "Internal error"; break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED: reason = "Feature unsupported"; break;
                default: reason = "Error code " + errorCode; break;
            }
            Log.e(TAG, "BLE advertising failed: " + reason);
            isAdvertising = false;
            JSObject data = new JSObject();
            data.put("event", "advertiseFail");
            data.put("reason", reason);
            notifyListeners("bleEvent", data);
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            try {
                String address = device != null ? device.getAddress() : "unknown";
                Log.d(TAG, "Connection state change: " + address + " state=" + newState);
                JSObject data = new JSObject();
                data.put("event", newState == BluetoothGatt.STATE_CONNECTED ? "connected" : "disconnected");
                data.put("deviceAddress", address);
                notifyListeners("bleEvent", data);
            } catch (Exception e) {
                Log.w(TAG, "onConnectionStateChange error", e);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(
            BluetoothDevice device, int requestId,
            BluetoothGattCharacteristic characteristic,
            boolean preparedWrite, boolean responseNeeded,
            int offset, byte[] value
        ) {
            try {
                if (CHAR_WRITE_UUID.equals(characteristic.getUuid()) && value != null) {
                    String studentNo = new String(value, StandardCharsets.UTF_8).trim();
                    String address = device != null ? device.getAddress() : "unknown";
                    // Clear previous response per device so student doesn't read stale data
                    bleResponseMap.put(address, "PENDING");
                    Log.d(TAG, "Received student number: " + studentNo + " from " + address);

                    // Get RSSI and distance for this device
                    Integer rssi = deviceRssi.get(address);
                    int rssiVal = rssi != null ? rssi : -80;
                    double distance = rssiToDistance(rssiVal);
                    String distLabel = distanceLabel(distance);

                    JSObject data = new JSObject();
                    data.put("event", "checkin");
                    data.put("studentNo", studentNo);
                    data.put("deviceAddress", address);
                    data.put("rssi", rssiVal);
                    data.put("distance", Math.round(distance * 10.0) / 10.0);
                    data.put("distanceLabel", distLabel);
                    data.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
                    notifyListeners("bleEvent", data);

                    if (responseNeeded && gattServer != null) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, "OK".getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    if (responseNeeded && gattServer != null) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "onCharacteristicWriteRequest error", e);
            }
        }

        @Override
        public void onCharacteristicReadRequest(
            BluetoothDevice device, int requestId, int offset,
            BluetoothGattCharacteristic characteristic
        ) {
            try {
                if (CHAR_READ_UUID.equals(characteristic.getUuid()) && gattServer != null) {
                    // Return per-device response (set by JS after processing check-in)
                    String deviceAddr = device != null ? device.getAddress() : "";
                    String deviceResponse = bleResponseMap.get(deviceAddr);
                    String responseStr = (deviceResponse != null && !deviceResponse.isEmpty()) ? deviceResponse : courseInfo;
                    byte[] response = responseStr.getBytes(StandardCharsets.UTF_8);
                    byte[] slice = offset < response.length ? Arrays.copyOfRange(response, offset, response.length) : new byte[0];
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, slice);
                } else if (gattServer != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            } catch (Exception e) {
                Log.w(TAG, "onCharacteristicReadRequest error", e);
            }
        }
    };

    @Override
    protected void handleOnDestroy() {
        stopAll();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.handleOnDestroy();
    }
}
