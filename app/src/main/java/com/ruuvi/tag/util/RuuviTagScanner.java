package com.ruuvi.tag.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import com.ruuvi.tag.model.LeScanResult;
import com.ruuvi.tag.model.RuuviTag;

import java.util.ArrayList;
import java.util.List;

import static com.ruuvi.tag.RuuviScannerApplication.useNewApi;

public class RuuviTagScanner {
    private static final String TAG = "RuuviTagScanner";
    private RuuviTagListener listener;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ScanSettings scanSettings;
    private boolean scanning = false;

    public RuuviTagScanner(RuuviTagListener listener, Context context) {
        this.listener = listener;
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (useNewApi()) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            scanSettings = scanSettingsBuilder.build();
        }
    }

    public void start() {
        if (scanning) return;
        scanning = true;
        if (useNewApi()) {
            bleScanner.startScan(scanFilters, scanSettings, bleScannerCallback);
        } else {
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    public void stop() {
        if (useNewApi()) {
            bleScanner.stopScan(bleScannerCallback);
        } else {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        scanning = false;
    }

    @SuppressLint("NewApi")
    private ScanCallback bleScannerCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            foundDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    foundDevice(device, rssi, scanRecord);
                }
            };

    private void foundDevice(BluetoothDevice device, int rssi, byte[] data) {
        LeScanResult dev = new LeScanResult();
        dev.device = device;
        dev.rssi = rssi;
        dev.scanData = data;

        Log.d(TAG, "found: " + device.getAddress());
        RuuviTag tag = dev.parse();
        if (tag != null) listener.tagFound(tag);
    }

}
