package com.ruuvi.station.model;

import android.bluetooth.BluetoothDevice;

import com.neovisionaries.bluetooth.ble.advertising.ADManufacturerSpecific;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;

import java.util.List;

/**
 * Created by berg on 28/09/17.
 */

public class LeScanResult {
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanData;

    public RuuviTag parse() {
        RuuviTag tag = null;

        // Parse the payload of the advertisement packet
        // as a list of AD structures.
        List<ADStructure> structures =
                ADPayloadParser.getInstance().parse(this.scanData);

        // For each AD structure contained in the advertisement packet.
        for (ADStructure structure : structures) {
            if (structure instanceof EddystoneURL) {
                // Eddystone URL
                EddystoneURL es = (EddystoneURL) structure;
                if (es.getURL().toString().startsWith("https://ruu.vi/#") || es.getURL().toString().startsWith("https://r/")) {
                    tag = new RuuviTag(this.device.getAddress(), es.getURL().toString(), null, this.rssi, false);
                }
            }
            // If the AD structure represents Eddystone TLM.
            else if (structure instanceof ADManufacturerSpecific) {
                ADManufacturerSpecific es = (ADManufacturerSpecific) structure;
                if (es.getCompanyId() == 0x0499) {
                    byte[] data = es.getData();
                    if (data != null) {
                        tag = new RuuviTag(this.device.getAddress(), null, data, this.rssi, false);
                    }
                }
            }
        }

        return tag;
    }
}
