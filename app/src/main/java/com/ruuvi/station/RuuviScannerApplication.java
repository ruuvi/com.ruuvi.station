package com.ruuvi.station;

import android.app.Application;
import android.content.Context;

import com.ruuvi.station.bluetooth.BluetoothInteractor;
import com.ruuvi.station.bluetooth.gateway.BluetoothForegroundServiceGateway;

import org.altbeacon.bluetooth.BluetoothMedic;


public class RuuviScannerApplication extends Application {

    private BluetoothInteractor bluetoothInteractor = new BluetoothInteractor(this);

    public final BluetoothForegroundServiceGateway bluetoothForegroundServiceGateway =
            new BluetoothForegroundServiceGateway(this);

    public void startForegroundScanning() {
        bluetoothInteractor.startForegroundScanning();
    }

    public void startBackgroundScanning() {
        bluetoothInteractor.startBackgroundScanning();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothInteractor.onCreate();
    }
}
