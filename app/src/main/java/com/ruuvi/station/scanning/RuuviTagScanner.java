package com.ruuvi.station.scanning;

import android.content.Context;

import com.ruuvi.station.RuuviScannerApplication;
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothScanningGateway;

public class RuuviTagScanner {

    private final BluetoothScanningGateway bluetoothScanningGateway;

    private final RuuviTagListener listener;

    public RuuviTagScanner(RuuviTagListener listener, Context context) {
        this.listener = listener;
        this.bluetoothScanningGateway = ((RuuviScannerApplication) context.getApplicationContext())
                .bluetoothScanningGatewayFactory.create();
    }

    public void start() {
        bluetoothScanningGateway.startScan(listener);
    }

    public void stop() {
        bluetoothScanningGateway.stopScan();
    }
}