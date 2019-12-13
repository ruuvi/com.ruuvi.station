package com.ruuvi.station.service;

import android.content.Context;
import android.util.Log;

import com.raizlabs.android.dbflow.data.Blob;
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway;
import com.ruuvi.station.decoder.DecodeFormat2and4;
import com.ruuvi.station.decoder.DecodeFormat3;
import com.ruuvi.station.decoder.DecodeFormat5;
import com.ruuvi.station.decoder.RuuviTagDecoder;
import com.ruuvi.station.model.HumidityCalibration;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.util.Utils;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RuuviRangeNotifier implements RangeNotifier {

    private static final String TAG = "RuuviRangeNotifier";

    private String from;
    private Context context;
    private long last = 0;

    @Nullable
    private BluetoothTagGateway.OnTagsFoundListener onTagsFoundListener;

    public RuuviRangeNotifier(
            Context context,
            String from,
            @Nullable BluetoothTagGateway.OnTagsFoundListener onTagsFoundListener
    ) {
        this.onTagsFoundListener = onTagsFoundListener;
        Log.d(TAG, "Setting up range notifier from " + from);
        this.context = context;
        this.from = from;
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        long now = System.currentTimeMillis();
        if (now <= last + 500) {
            Log.d(TAG, "Double range bug");
            return;
        }
        last = now;

        final List<RuuviTag> allTags = new ArrayList<>();
        final List<RuuviTag> tagsToSend = new ArrayList<>();

        Log.d(TAG, from + " " + " found " + beacons.size());

        foundBeacon: for (Beacon beacon : beacons) {
            // the same tag can appear multiple times
            for (RuuviTag tag : tagsToSend) {
                if (tag.id.equals(beacon.getBluetoothAddress())) continue foundBeacon;
            }
            RuuviTag tag = fromAltbeacon(context, beacon);
            if (tag != null) {
                if (tag.favorite) tagsToSend.add(tag);
                allTags.add(tag);
            }
        }

        if (onTagsFoundListener != null) {
            onTagsFoundListener.onFoundTags(allTags);
        }
    }

    public static RuuviTag fromAltbeacon(Context context, Beacon beacon) {
        try {
            byte pData[] = new byte[128];
            List<Long> data = beacon.getDataFields();
            for (int i = 0; i < data.size(); i++)
                pData[i] = (byte)(data.get(i) & 0xFF);
            RuuviTagDecoder decoder = null;
            String url = null;
            int format = beacon.getBeaconTypeCode();
            if (data.size() > 0) format = pData[0];
            switch (format) {
                case 3:
                    decoder = new DecodeFormat3();
                    break;
                case 5:
                    decoder = new DecodeFormat5();
                    break;
                case 0x10:
                    // format 2 & 4
                    if (beacon.getServiceUuid() == 0xfeaa) {
                        if (beacon.getId1() == null) break;
                        url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                        if (url.contains("https://ruu.vi/#")) {
                            String urlData = url.split("#")[1];
                            pData = Utils.parseByteDataFromB64(urlData);
                            decoder = new DecodeFormat2and4();
                        }
                    }
                    break;
            }
            if (decoder != null) {
                try {
                    RuuviTag tag = decoder.decode(pData, 0);
                    tag.id = beacon.getBluetoothAddress();
                    tag.rssi = beacon.getRssi();
                    tag.url = url;
                    tag.rawData = pData;
                    tag.rawDataBlob = new Blob(pData);
                    tag = HumidityCalibration.apply(tag);
                    //Log.d(TAG, "logged tag with format: " + tag.dataFormat + " and mac: " + tag.id + " temp: " + tag.temperature);
                    return tag;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse tag data");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse ruuviTag");
        }
        return null;
    }
}