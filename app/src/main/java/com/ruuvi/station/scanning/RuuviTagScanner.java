package com.ruuvi.station.scanning;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class RuuviTagScanner {
    private static final String TAG = "RuuviTagScanner";
    private RuuviTagListener listener;

    private boolean scanning = false;

    private ScanSettings scanSettings;
    private BluetoothLeScannerCompat scanner;

    public RuuviTagScanner(RuuviTagListener listener, Context context) {
        this.listener = listener;
        scanSettings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(false).build();
        scanner = BluetoothLeScannerCompat.getScanner();
    }

    public void start() {
        if (scanning || !canScan()) return;
        scanning = true;
        scanner.startScan(null, scanSettings, nsCallback);
    }

    public void stop() {
        if (!canScan()) return;
        scanning = false;
        scanner.stopScan(nsCallback);
    }

    private ScanCallback nsCallback = new no.nordicsemi.android.support.v18.scanner.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, no.nordicsemi.android.support.v18.scanner.ScanResult result) {
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
