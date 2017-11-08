package com.ruuvi.tag.scanning;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.ruuvi.tag.feature.main.MainActivity;
import com.ruuvi.tag.model.LeScanResult;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.model.ScanEvent;
import com.ruuvi.tag.model.ScanEventSingle;
import com.ruuvi.tag.util.AlarmChecker;
import com.ruuvi.tag.util.DeviceIdentifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static com.ruuvi.tag.RuuviScannerApplication.useNewApi;
import static com.ruuvi.tag.service.ScannerService.logTag;

/**
 * Created by berg on 30/09/17.
 */

public class BackgroundScanner extends BroadcastReceiver {
    private static final String TAG = "BackgroundScanner";
    public static final int REQUEST_CODE = 9001;
    private static final int SCAN_TIME_MS = 5000;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ScanSettings scanSettings;
    private List<LeScanResult> scanResults;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(final Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        Log.d(TAG, "Woke up");
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (useNewApi()) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            scanSettings = scanSettingsBuilder.build();
        }
        scanResults = new ArrayList<LeScanResult>();

        //MainActivity.enableBluetooth();

        if (!canScan()) {
            Log.d(TAG, "Could not start scanning in background, scheduling next attempt");
            scheduleNextScan(context);
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (useNewApi()) {
                    bleScanner.stopScan(bleScannerCallback);
                } else {
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                }
                processFoundDevices(context);
                scanResults = new ArrayList<LeScanResult>();
            }
        }, SCAN_TIME_MS);

        if (useNewApi()) {
            bleScanner.startScan(scanFilters, scanSettings, bleScannerCallback);
        } else {
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }
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
        Iterator<LeScanResult> itr = scanResults.iterator();
        LeScanResult dev = new LeScanResult();
        dev.device = device;
        dev.rssi = rssi;
        dev.scanData = data;

        boolean devFound = false;
        while (itr.hasNext()) {
            LeScanResult element = itr.next();
            if (device.getAddress().equalsIgnoreCase(element.device.getAddress()))
                devFound = true;
        }

        if (!devFound) {
            Log.d(TAG, "Found: " + device.getAddress());
            scanResults.add(dev);
        }
    }

    void processFoundDevices(Context context) {
        ScanEvent scanEvent = new ScanEvent(context, DeviceIdentifier.id(context));

        Iterator<LeScanResult> itr = scanResults.iterator();
        while (itr.hasNext()) {
            LeScanResult element = itr.next();

            RuuviTag tag  = element.parse();
            if (tag != null) addFoundTagToLists(tag, scanEvent);
        }

        Log.d(TAG, "Found " + scanEvent.tags.size() + " tags");

        ScanEvent eventBatch = new ScanEvent(scanEvent.deviceId, scanEvent.time);
        for (int i = 0; i < scanEvent.tags.size(); i++) {
            RuuviTag tagFromDb = RuuviTag.get(scanEvent.tags.get(i).id);
            // don't send data about tags not in the list
            if (tagFromDb == null) continue;

            eventBatch.tags.add(tagFromDb);

            if (tagFromDb.gatewayUrl != null && !tagFromDb.gatewayUrl.isEmpty()) {
                // send the single tag to its gateway
                ScanEventSingle single = new ScanEventSingle(scanEvent.deviceId, scanEvent.time);
                single.tag = tagFromDb;

                Ion.with(context)
                        .load(tagFromDb.gatewayUrl)
                        .setJsonPojoBody(single)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {
                                if (e != null) {
                                    Log.e(TAG, "Sending failed.");
                                }
                            }
                        });
            }

            AlarmChecker.check(tagFromDb, context);
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String backendUrl = settings.getString("pref_backend", null);

        if (backendUrl != null && eventBatch.tags.size() > 0)
        {
            Ion.with(context)
                    .load(backendUrl)
                    .setJsonPojoBody(eventBatch)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (e != null) {
                                Log.e(TAG, "Batch sending failed.");
                            }
                        }
                    });
        }

        scheduleNextScan(context);

        Log.d(TAG, "Going to sleep");
        wakeLock.release();
    }

    private void scheduleNextScan(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "300")) * 1000;
        if (scanInterval < 15 * 1000) scanInterval = 15 * 1000;
        boolean batterySaving = settings.getBoolean("pref_bgscan_battery_saving", false);

        Intent intent = new Intent(context, BackgroundScanner.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, BackgroundScanner.REQUEST_CODE, intent, 0);
        AlarmManager am = (AlarmManager) context
                .getSystemService(ALARM_SERVICE);
        if (!batterySaving) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scanInterval, sender);
            }
            else {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scanInterval, sender);
            }
        }

    }

    private int checkForSameTag(List<RuuviTag> arr, RuuviTag ruuvi) {
        for (int i = 0; i < arr.size(); i++) {
            if (ruuvi.id.equals(arr.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    public void addFoundTagToLists(RuuviTag tag, ScanEvent scanEvent) {
        int index = checkForSameTag(scanEvent.tags, tag);
        if (index == -1) {
            scanEvent.tags.add(tag);
            logTag(tag);
        }
    }

    private boolean canScan() {
        boolean useNewApi = useNewApi();
        return useNewApi && bleScanner != null || !useNewApi && bluetoothAdapter != null;
    }
}
