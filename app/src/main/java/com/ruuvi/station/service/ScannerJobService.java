package com.ruuvi.station.service;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.ruuvi.station.gateway.Http;
import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.ruuvi.station.service.ScannerService.logTag;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScannerJobService extends JobService {
    private static final String TAG = "ScannerJobService";
    public static final int REQUEST_CODE = 9001;
    private static final int SCAN_TIME_MS = 5000;

    private List<LeScanResult> scanResults;
    //private PowerManager.WakeLock wakeLock;


    private BluetoothAdapter bluetoothAdapter;
    private ScanSettings scanSettings;
    private BluetoothLeScanner scanner;
    private Location tagLocation;
    private JobParameters jobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Woke up");
        this.jobParameters = jobParameters;

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                .build();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

        scanResults = new ArrayList<>();

        if (!canScan()) {
            Log.d(TAG, "Could not start scanning in background, scheduling next attempt");
            return false;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(nsCallback);
                processFoundDevices(getApplicationContext());
                scanResults = new ArrayList<LeScanResult>();
            }
        }, SCAN_TIME_MS);

        try {
            scanner.startScan(Utils.getScanFilters(), scanSettings, nsCallback);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return true;
    }

    private ScanCallback nsCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
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
        List<RuuviTag> tags = new ArrayList<>();
        Iterator<LeScanResult> itr = scanResults.iterator();
        while (itr.hasNext()) {
            LeScanResult element = itr.next();

            RuuviTag tag  = element.parse(context);
            if (tag != null) addFoundTagToLists(tag, tags, context);
        }

        Log.d(TAG, "Found " + tags.size() + " tags");

        Http.post(tags, tagLocation, context);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Going to sleep");
                jobFinished(jobParameters, false);
            }
        }, 1000);
    }

    private int checkForSameTag(List<RuuviTag> arr, RuuviTag ruuvi) {
        for (int i = 0; i < arr.size(); i++) {
            if (ruuvi.id.equals(arr.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    public void addFoundTagToLists(RuuviTag tag, List<RuuviTag> tags, Context context) {
        int index = checkForSameTag(tags, tag);
        if (index == -1) {
            tags.add(tag);
            logTag(tag, context, true);
        }
    }


    private boolean canScan() {
        return scanner != null;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
