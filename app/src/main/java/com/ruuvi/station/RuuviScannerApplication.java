package com.ruuvi.station;

import android.app.Application;
import android.util.Log;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.ruuvi.station.bluetooth.BluetoothInteractor;
import com.ruuvi.station.bluetooth.gateway.factory.BackgroundBluetoothScannerGatewayFactory;
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothScanningGatewayFactory;
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBackgroundBluetoothScannerGatewayFactory;
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothScanningGatewayFactory;
import com.ruuvi.station.bluetooth.model.factory.DefaultLeScanResultFactory;
import com.ruuvi.station.bluetooth.model.factory.LeScanResultFactory;


public class RuuviScannerApplication extends Application {

    private static final String TAG = "RuuviScannerApplication";

    public BluetoothInteractor bluetoothInteractor = new BluetoothInteractor(this);

    public BackgroundBluetoothScannerGatewayFactory scannerGatewayFactory =
            new DefaultBackgroundBluetoothScannerGatewayFactory(this);

    public BluetoothScanningGatewayFactory bluetoothScanningGatewayFactory =
            new DefaultBluetoothScanningGatewayFactory(this);

    public final LeScanResultFactory leScanResultFactory = new DefaultLeScanResultFactory(this);

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "App class onCreate");

        bluetoothInteractor.onAppCreated();

        FlowManager.init(getApplicationContext());
    }
}
