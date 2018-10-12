package com.ruuvi.station.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.ruuvi.station.RuuviScannerApplication;
import com.ruuvi.station.util.BackgroundScanModes;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.ServiceUtils;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;


public class AltBeaconScannerService extends Service implements BeaconConsumer {
    private static final String TAG = "AScannerService";

    private int backgroundScanInterval = Constants.DEFAULT_SCAN_INTERVAL;
    private BeaconManager beaconManager;
    private Region region;
    private RuuviRangeNotifier ruuviRangeNotifier;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Foreground.init(getApplication());
        Foreground.get().addListener(listener);

        Log.d(TAG, "Starting service");
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));
        region = new Region("com.ruuvi.station.leRegion", null, null, null);
        beaconManager.bind(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy =======");
        if (beaconManager != null) {
            try {
                beaconManager.removeAllRangeNotifiers();
                beaconManager.stopRangingBeaconsInRegion(region);
            } catch (Exception e) {
                Log.d(TAG, "Could not remove ranging region");
            }
            beaconManager.unbind(this);
            beaconManager = null;
        }
        Preferences prefs = new Preferences(this);
        if (prefs.getBackgroundScanMode() == BackgroundScanModes.FOREGROUND) {
            new ServiceUtils(this).startForegroundService();
        } else if (prefs.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
            ((RuuviScannerApplication)(this.getApplication())).startBackgroundScanning();
        }
        if (listener != null) Foreground.get().removeListener(listener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
        }

        public void onBecameBackground() {
            //onDestroy();
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        if (ruuviRangeNotifier == null) ruuviRangeNotifier = new RuuviRangeNotifier(this, "AltBeaconScannerService");
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(ruuviRangeNotifier);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not start ranging");
        }
    }
}
