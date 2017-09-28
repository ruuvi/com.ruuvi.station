package com.ruuvi.tag.model;

import android.bluetooth.BluetoothDevice;

/**
 * Created by berg on 28/09/17.
 */

public class LeScanResult {
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanData;
}
