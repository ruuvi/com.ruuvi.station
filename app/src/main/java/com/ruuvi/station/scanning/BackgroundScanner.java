package com.ruuvi.station.scanning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.ScanEvent;
import com.ruuvi.station.model.ScanEventSingle;
import com.ruuvi.station.model.ScanLocation;
import com.ruuvi.station.util.DeviceIdentifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.Context.ALARM_SERVICE;
import static com.ruuvi.station.service.ScannerService.logTag;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

/**
 * Created by berg on 30/09/17.
 */

public class BackgroundScanner extends BroadcastReceiver {
    private static final String TAG = "BackgroundScanner";
    public static final int REQUEST_CODE = 9001;
    private static final int SCAN_TIME_MS = 5000;

    private List<LeScanResult> scanResults;
    //private PowerManager.WakeLock wakeLock;

    private ScanSettings scanSettings;
    private BluetoothLeScannerCompat scanner;
    private Location tagLocation;

    @Override
    public void onReceive(final Context context, Intent intent) {
        /*
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
        */
        Log.d(TAG, "Woke up");
        scheduleNextScan(context);

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    tagLocation = location;
                }
            });
        }

        scanSettings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(false).build();

        scanner = BluetoothLeScannerCompat.getScanner();

        scanResults = new ArrayList<>();

        if (!canScan()) {
            Log.d(TAG, "Could not start scanning in background, scheduling next attempt");
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(nsCallback);
                processFoundDevices(context);
                scanResults = new ArrayList<LeScanResult>();
            }
        }, SCAN_TIME_MS);

        try {
            scanner.startScan(null, scanSettings, nsCallback);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private ScanCallback nsCallback = new no.nordicsemi.android.support.v18.scanner.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, no.nordicsemi.android.support.v18.scanner.ScanResult result) {
            super.onScanResult(callbackType, result);
            foundDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
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

        ScanLocation location = null;
        if (tagLocation != null) {
            location = new ScanLocation();
            location.latitude = tagLocation.getLatitude();
            location.longitude = tagLocation.getLongitude();
            location.accuracy = tagLocation.getAccuracy();
        }
        scanEvent.location = location;


        Iterator<LeScanResult> itr = scanResults.iterator();
        while (itr.hasNext()) {
            LeScanResult element = itr.next();

            RuuviTag tag  = element.parse();
            if (tag != null) addFoundTagToLists(tag, scanEvent, context);
        }

        Log.d(TAG, "Found " + scanEvent.tags.size() + " tags");

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
        Ion.getDefault(context).configure().setGson(gson);

        ScanEvent eventBatch = new ScanEvent(scanEvent.deviceId, scanEvent.time);
        eventBatch.location = location;
        for (int i = 0; i < scanEvent.tags.size(); i++) {
            RuuviTag tagFromDb = RuuviTag.get(scanEvent.tags.get(i).id);
            // don't send data about tags not in the list
            if (tagFromDb == null || !tagFromDb.favorite) continue;

            eventBatch.tags.add(tagFromDb);

            if (tagFromDb.gatewayUrl != null && !tagFromDb.gatewayUrl.isEmpty()) {
                // send the single tag to its gateway
                ScanEventSingle single = new ScanEventSingle(scanEvent.deviceId, scanEvent.time);
                single.location = location;
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

        Log.d(TAG, "Going to sleep");
        //wakeLock.release();
    }

    private void scheduleNextScan(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "30")) * 1000;
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

    public void addFoundTagToLists(RuuviTag tag, ScanEvent scanEvent, Context context) {
        int index = checkForSameTag(scanEvent.tags, tag);
        if (index == -1) {
            scanEvent.tags.add(tag);
            logTag(tag, context);
        }
    }

    private boolean canScan() {
        return scanner != null;
    }
}
