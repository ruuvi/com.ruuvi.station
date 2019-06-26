package com.ruuvi.station.scanning;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.util.Utils;

public class RuuviTagScanner {
    private static final String TAG = "RuuviTagScanner";
    private RuuviTagListener listener;

    private BluetoothAdapter bluetoothAdapter;
    private ScanSettings scanSettings;
    private BluetoothLeScanner scanner;
    private boolean scanning = false;

    public RuuviTagScanner(RuuviTagListener listener, Context context) {
        this.listener = listener;

        scanSettings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

    }

    public void start() {
        if (scanning || !canScan()) return;
        scanning = true;
        scanner.startScan(Utils.getScanFilters(), scanSettings, nsCallback);
    }

    public void stop() {
        if (!canScan()) return;
        scanning = false;
        scanner.stopScan(nsCallback);
    }

    private ScanCallback nsCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            foundDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
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

    private boolean canScan() {
        return scanner != null;
    }
}
