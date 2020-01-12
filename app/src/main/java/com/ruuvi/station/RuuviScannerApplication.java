package com.ruuvi.station;

import android.app.Application;
import android.content.Context;

import com.ruuvi.station.bluetooth.BluetoothInteractor;

import org.altbeacon.bluetooth.BluetoothMedic;


public class RuuviScannerApplication extends Application {

    private BluetoothInteractor bluetoothInteractor = new BluetoothInteractor(this);

    public void startForegroundScanning() {
        bluetoothInteractor.startForegroundScanning();
    }

    public void startBackgroundScanning() {
        bluetoothInteractor.startBackgroundScanning();
    }

    public static BluetoothMedic setupMedic(Context context) {
        BluetoothMedic medic = BluetoothMedic.getInstance();
        medic.enablePowerCycleOnFailures(context);
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST);
        return medic;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothInteractor.onCreate();
    }
}
