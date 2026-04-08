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
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import java.nio.charset.StandardCharsets;
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

    // Must match the student check-in page
    public static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    public static final UUID CHAR_WRITE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd");
    public static final UUID CHAR_READ_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServer gattServer;
    private boolean isAdvertising = false;
    private String courseInfo = "";

    @PluginMethod
    public void startAdvertising(PluginCall call) {
        String course = call.getString("course", "SmartAttendance");
        courseInfo = course;

        bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            call.reject("Bluetooth is not enabled");
            return;
        }

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            call.reject("BLE advertising not supported on this device");
            return;
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

        AdvertiseData data = new AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(new ParcelUuid(SERVICE_UUID))
            .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build();

        try {
            advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback);
            isAdvertising = true;

            JSObject ret = new JSObject();
            ret.put("status", "advertising");
            ret.put("course", course);
            ret.put("serviceUuid", SERVICE_UUID.toString());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to start advertising: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopAdvertising(PluginCall call) {
        stopAll();
        JSObject ret = new JSObject();
        ret.put("status", "stopped");
        call.resolve(ret);
    }

    @PluginMethod
    public void isAdvertising(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("advertising", isAdvertising);
        call.resolve(ret);
    }

    private void startGattServer() {
        if (gattServer != null) {
            gattServer.close();
        }

        gattServer = bluetoothManager.openGattServer(getContext(), gattServerCallback);

        // Create the attendance service
        BluetoothGattService service = new BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        );

        // Writable characteristic — students write their student number here
        BluetoothGattCharacteristic writeChar = new BluetoothGattCharacteristic(
            CHAR_WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        service.addCharacteristic(writeChar);

        // Readable characteristic — professor's course info for verification
        BluetoothGattCharacteristic readChar = new BluetoothGattCharacteristic(
            CHAR_READ_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        );
        readChar.setValue(courseInfo.getBytes(StandardCharsets.UTF_8));
        service.addCharacteristic(readChar);

        gattServer.addService(service);
        Log.d(TAG, "GATT server started with service: " + SERVICE_UUID);
    }

    private void stopAll() {
        if (advertiser != null && isAdvertising) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
            } catch (Exception e) {
                Log.w(TAG, "Stop advertising error: " + e.getMessage());
            }
        }
        if (gattServer != null) {
            gattServer.close();
            gattServer = null;
        }
        isAdvertising = false;
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertising started successfully");
            isAdvertising = true;
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "BLE advertising failed with error: " + errorCode);
            isAdvertising = false;
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, "Connection state change: " + device.getAddress() + " state=" + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                JSObject data = new JSObject();
                data.put("event", "connected");
                data.put("deviceAddress", device.getAddress());
                notifyListeners("bleEvent", data);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                JSObject data = new JSObject();
                data.put("event", "disconnected");
                data.put("deviceAddress", device.getAddress());
                notifyListeners("bleEvent", data);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(
            BluetoothDevice device, int requestId,
            BluetoothGattCharacteristic characteristic,
            boolean preparedWrite, boolean responseNeeded,
            int offset, byte[] value
        ) {
            if (CHAR_WRITE_UUID.equals(characteristic.getUuid())) {
                String studentNo = new String(value, StandardCharsets.UTF_8).trim();
                Log.d(TAG, "Received student number: " + studentNo + " from " + device.getAddress());

                // Notify the web layer
                JSObject data = new JSObject();
                data.put("event", "checkin");
                data.put("studentNo", studentNo);
                data.put("deviceAddress", device.getAddress());
                data.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
                notifyListeners("bleEvent", data);

                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, "OK".getBytes(StandardCharsets.UTF_8));
                }
            } else {
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(
            BluetoothDevice device, int requestId, int offset,
            BluetoothGattCharacteristic characteristic
        ) {
            if (CHAR_READ_UUID.equals(characteristic.getUuid())) {
                byte[] response = courseInfo.getBytes(StandardCharsets.UTF_8);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    offset < response.length ? java.util.Arrays.copyOfRange(response, offset, response.length) : new byte[0]);
            } else {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }
    };

    @Override
    protected void handleOnDestroy() {
        stopAll();
        super.handleOnDestroy();
    }
}
