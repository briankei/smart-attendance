package com.briankei.smartattendance;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "MainActivity";
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BleAttendancePlugin.class);
        super.onCreate(savedInstanceState);

        // Setup NFC foreground dispatch
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            nfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flags);
            Log.d(TAG, "NFC adapter found");
        } else {
            Log.w(TAG, "NFC not available on this device");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            try {
                nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent,
                    new IntentFilter[]{ new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) }, null);
            } catch (Exception e) {
                Log.w(TAG, "enableForegroundDispatch failed", e);
            }
        }
    }

    @Override
    public void onPause() {
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableForegroundDispatch(this);
            } catch (Exception e) {
                Log.w(TAG, "disableForegroundDispatch failed", e);
            }
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
            || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()))) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                byte[] idBytes = tag.getId();
                StringBuilder sb = new StringBuilder();
                for (byte b : idBytes) {
                    sb.append(String.format("%02x", b));
                    sb.append(':');
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1);
                String serial = sb.toString();
                Log.d(TAG, "NFC tag: " + serial);

                // Send to WebView via JavaScript
                String js = "if(window._onNativeNFC){window._onNativeNFC('" + serial + "');}";
                getBridge().getWebView().post(() ->
                    getBridge().getWebView().evaluateJavascript(js, null));
            }
        }
    }
}
