//package com.ruuvi.station.scanning;
//
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanResult;
//import android.bluetooth.le.ScanSettings;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.location.Location;
//import android.os.Build;
//import android.os.Handler;
//import android.os.SystemClock;
//import android.support.v4.content.ContextCompat;
//import android.util.Log;
//
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.ruuvi.station.gateway.Http;
//import com.ruuvi.station.model.LeScanResult;
//import com.ruuvi.station.model.RuuviTag;
//import com.ruuvi.station.util.Preferences;
//import com.ruuvi.station.util.Utils;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//import static android.content.Context.ALARM_SERVICE;
//import static com.ruuvi.station.service.ScannerService.logTag;
//
///**
// * Created by io53 on 30/09/17.
// */
//public class BackgroundScanner extends BroadcastReceiver {
//    private static final String TAG = "BackgroundScanner";
//    public static final int REQUEST_CODE = 9001;
//    private static final int SCAN_TIME_MS = 5000;
//
//    private List<LeScanResult> scanResults;
//    //private PowerManager.WakeLock wakeLock;
//
//    private BluetoothAdapter bluetoothAdapter;
//    private ScanSettings scanSettings;
//    private BluetoothLeScanner scanner;
//    private Location tagLocation;
//
//    @Override
//    public void onReceive(final Context context, Intent intent) {
//        /*
//        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                "MyWakelockTag");
//        wakeLock.acquire();
//        */
//        Log.d(TAG, "Woke up");
//        scheduleNextScan(context);
//
//        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
//        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
//                @Override
//                public void onSuccess(Location location) {
//                    tagLocation = location;
//                }
//            });
//        }
//
//        scanSettings = new ScanSettings.Builder()
//                .setReportDelay(0)
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                .build();
//
//        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
//        bluetoothAdapter = bluetoothManager.getAdapter();
//        scanner = bluetoothAdapter.getBluetoothLeScanner();
//
//        scanResults = new ArrayList<>();
//
//        if (!canScan()) {
//            Log.d(TAG, "Could not start scanning in background, scheduling next attempt");
//            return;
//        }
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                scanner.stopScan(nsCallback);
//                processFoundDevices(context);
//                scanResults = new ArrayList<LeScanResult>();
//            }
//        }, SCAN_TIME_MS);
//
//        try {
//            scanner.startScan(Utils.getScanFilters(), scanSettings, nsCallback);
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage());
//        }
//    }
//
//    private ScanCallback nsCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            super.onScanResult(callbackType, result);
//            foundDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
//        }
//    };
//
//    private void foundDevice(BluetoothDevice device, int rssi, byte[] data) {
//        Iterator<LeScanResult> itr = scanResults.iterator();
//        LeScanResult dev = new LeScanResult();
//        dev.device = device;
//        dev.rssi = rssi;
//        dev.scanData = data;
//
//        boolean devFound = false;
//        while (itr.hasNext()) {
//            LeScanResult element = itr.next();
//            if (device.getAddress().equalsIgnoreCase(element.device.getAddress()))
//                devFound = true;
//        }
//
//        if (!devFound) {
//            Log.d(TAG, "Found: " + device.getAddress());
//            scanResults.add(dev);
//        }
//    }
//
//    void processFoundDevices(Context context) {
//        List<RuuviTag> tags = new ArrayList<>();
//        Iterator<LeScanResult> itr = scanResults.iterator();
//        while (itr.hasNext()) {
//            LeScanResult element = itr.next();
//
//            RuuviTag tag  = element.parse(context);
//            if (tag != null) addFoundTagToLists(tag, tags, context);
//        }
//
//        Log.d(TAG, "Found " + tags.size() + " tags");
//
//        Http.post(tags, tagLocation, context);
//
//        Log.d(TAG, "Going to sleep");
//        //wakeLock.release();
//    }
//
//    private void scheduleNextScan(Context context) {
//        Preferences prefs = new Preferences(context);
//        //int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "30")) * 1000;
//        int scanInterval = prefs.getBackgroundScanInterval() * 1000;
//        if (scanInterval < 15 * 1000) scanInterval = 15 * 1000;
//        boolean batterySaving = prefs.getBatterySaverEnabled();
//
//        Intent intent = new Intent(context, BackgroundScanner.class);
//        PendingIntent sender = PendingIntent.getBroadcast(context, BackgroundScanner.REQUEST_CODE, intent, 0);
//        AlarmManager am = (AlarmManager) context
//                .getSystemService(ALARM_SERVICE);
//        if (!batterySaving) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scanInterval, sender);
//            }
//            else {
//                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scanInterval, sender);
//            }
//        }
//    }
//
//    private int checkForSameTag(List<RuuviTag> arr, RuuviTag ruuvi) {
//        for (int i = 0; i < arr.size(); i++) {
//            if (ruuvi.id.equals(arr.get(i).id)) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    public void addFoundTagToLists(RuuviTag tag, List<RuuviTag> tags, Context context) {
//        int index = checkForSameTag(tags, tag);
//        if (index == -1) {
//            tags.add(tag);
//            logTag(tag, context, true);
//        }
//    }
//
//    private boolean canScan() {
//        return scanner != null;
//    }
//}
