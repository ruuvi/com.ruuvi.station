package com.ruuvi.station.model;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.neovisionaries.bluetooth.ble.advertising.ADManufacturerSpecific;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.raizlabs.android.dbflow.data.Blob;
import com.ruuvi.station.bluetooth.model.LeScanResult;
import com.ruuvi.station.decoder.DecodeFormat2and4;
import com.ruuvi.station.decoder.DecodeFormat3;
import com.ruuvi.station.decoder.DecodeFormat5;
import com.ruuvi.station.decoder.RuuviTagDecoder;
import com.ruuvi.station.util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by berg on 28/09/17.
 */

public class NeovisionariesLeScanResult implements LeScanResult {

    private static final String TAG = NeovisionariesLeScanResult.class.getSimpleName();
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanData;

    @Override
    public RuuviTag parse(Context context) {
        RuuviTag tag = null;

        try {
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
                        tag = from(context, this.device.getAddress(), es.getURL().toString(), null, this.rssi);
                    }
                }
                // If the AD structure represents Eddystone TLM.
                else if (structure instanceof ADManufacturerSpecific) {
                    ADManufacturerSpecific es = (ADManufacturerSpecific) structure;
                    if (es.getCompanyId() == 0x0499) {
                        tag = from(context, this.device.getAddress(), null, this.scanData, this.rssi);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parsing ble data failed");
        }
        if (tag != null) {
            tag = HumidityCalibration.apply(tag);
        }

        return tag;
    }

    private static RuuviTag from(Context context, String id, String url, byte[] rawData, int rssi) {
        RuuviTagDecoder decoder = null;
        if (url != null && url.contains("#")) {
            String data = url.split("#")[1];
            rawData = Utils.parseByteDataFromB64(data);
            decoder = new DecodeFormat2and4();
        } else if (rawData != null) {
            int protocolVersion = rawData[7];
            switch (protocolVersion) {
                case 3:
                    decoder = new DecodeFormat3();
                    break;
                case 5:
                    decoder = new DecodeFormat5();
                    break;
            }
        }
        if (decoder != null) {
            RuuviTag tag = decoder.decode(rawData, 7);
            if (tag != null) {
                tag.id = id;
                tag.url = url;
                tag.rssi = rssi;
                tag.rawData = rawData;
                tag.rawDataBlob = new Blob(rawData);
                tag = HumidityCalibration.apply(tag);
            }
            return tag;
        }
        return null;
    }

    @Override
    public boolean hasSameDevice(@NotNull LeScanResult otherScanResult) {
        return device.getAddress().equalsIgnoreCase(
                ((NeovisionariesLeScanResult) otherScanResult).device.getAddress()
        );
    }
}
