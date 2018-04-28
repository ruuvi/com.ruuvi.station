package com.ruuvi.station.model;

import android.bluetooth.BluetoothDevice;

import com.neovisionaries.bluetooth.ble.advertising.ADManufacturerSpecific;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.raizlabs.android.dbflow.data.Blob;
import com.ruuvi.station.decoder.DecodeFormat2and4;
import com.ruuvi.station.decoder.DecodeFormat3;
import com.ruuvi.station.decoder.DecodeFormat5;
import com.ruuvi.station.decoder.RuuviTagDecoder;
import com.ruuvi.station.util.base64;

import java.util.Arrays;
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
                    tag = from(this.device.getAddress(), es.getURL().toString(), null, this.rssi);
                }
            }
            // If the AD structure represents Eddystone TLM.
            else if (structure instanceof ADManufacturerSpecific) {
                ADManufacturerSpecific es = (ADManufacturerSpecific) structure;
                if (es.getCompanyId() == 0x0499) {
                    tag = from(this.device.getAddress(), null, this.scanData, this.rssi);
                }
            }
        }

        return tag;
    }


    private static RuuviTag from(String id, String url, byte[] rawData, int rssi) {
        RuuviTagDecoder decoder = null;
        if (url != null && url.contains("#")) {
            String data = url.split("#")[1];
            rawData = parseByteDataFromB64(data);
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
            RuuviTag tag = decoder.decode(rawData);
            if (tag != null) {
                tag.id = id;
                tag.url = url;
                tag.rssi = rssi;
                tag.rawData = rawData;
                tag.rawDataBlob = new Blob(rawData);
            }
            return tag;
        }
        return null;
    }


    private static byte[] parseByteDataFromB64(String data) {
        try {
            byte[] bData = base64.decode(data);
            int pData[] = new int[8];
            for (int i = 0; i < bData.length; i++)
                pData[i] = bData[i] & 0xFF;
            return bData;
        } catch (Exception e) {
            return null;
        }
    }
}
