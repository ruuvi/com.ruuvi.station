package com.ruuvi.station.bluetooth;

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
import android.os.ParcelUuid;
import android.util.Log;

import com.ruuvi.station.bluetooth.interfaces.IRuuviTag;
import com.ruuvi.station.bluetooth.interfaces.IRuuviTagFactory;
import com.ruuvi.station.bluetooth.interfaces.IRuuviTagScanner;

import java.util.ArrayList;
import java.util.List;

public class RuuviTagScanner implements IRuuviTagScanner {

    private static final String TAG = "RuuviTagScanner";

    private RuuviTagListener listener;

    private ScanSettings scanSettings;
    private BluetoothLeScanner scanner;
    private boolean scanning = false;
    private IRuuviTagFactory ruuviTagFactory;
    private Context context;

    public RuuviTagScanner(RuuviTagListener listener, IRuuviTagFactory ruuviTagFactory, Context context) {
        this.listener = listener;
        this.ruuviTagFactory = ruuviTagFactory;
        this.context = context;

        scanSettings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

    }

    @Override
    @SuppressLint("MissingPermission")
    public void start() {
        if (scanning || !canScan()) return;
        scanning = true;
        scanner.startScan(getScanFilters(), scanSettings, nsCallback);
    }

    private static List<ScanFilter> getScanFilters() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter ruuviFilter = new ScanFilter.Builder()
                .setManufacturerData(0x0499, new byte[]{})
                .build();
        ScanFilter eddystoneFilter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb"))
                .build();
        filters.add(ruuviFilter);
        filters.add(eddystoneFilter);
        return filters;
    }

    @Override
    @SuppressLint("MissingPermission")
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
        LeScanResult result = new LeScanResult();
        result.device = device;
        result.rssi = rssi;
        result.scanData = data;

        Log.d(TAG, "found: " + device.getAddress());
        IRuuviTag tag = result.parse(context, ruuviTagFactory);
        if (tag != null) listener.tagFound(tag);
    }

    @Override
    public boolean canScan() {
        return scanner != null;
    }

    public interface RuuviTagListener {

        void tagFound(IRuuviTag tag);

    }
}
