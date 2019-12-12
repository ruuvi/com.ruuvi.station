package com.ruuvi.station.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.raizlabs.android.dbflow.data.Blob;
import com.ruuvi.station.decoder.DecodeFormat2and4;
import com.ruuvi.station.decoder.DecodeFormat3;
import com.ruuvi.station.decoder.DecodeFormat5;
import com.ruuvi.station.decoder.RuuviTagDecoder;
import com.ruuvi.station.gateway.Http;
import com.ruuvi.station.model.HumidityCalibration;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.TagSensorReading;
import com.ruuvi.station.util.AlarmChecker;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Utils;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuuviRangeNotifier implements RangeNotifier {

    private static final String TAG = "RuuviRangeNotifier";
    private String from;
    private Context context;
    private Location tagLocation;

    private Map<String, Long> lastLogged = null;
    public boolean gatewayOn = false;
    private FusedLocationProviderClient mFusedLocationClient;

    private long last = 0;

    public RuuviRangeNotifier(Context context, String from) {
        Log.d(TAG, "Setting up range notifier from " + from);
        this.context = context;
        this.from = from;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    private void updateLocation() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    tagLocation = location;
                }
            });
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        long now = System.currentTimeMillis();
        if (now <= last + 500) {
            Log.d(TAG, "Double range bug");
            return;
        }
        last = now;
        if (gatewayOn) updateLocation();
        List<RuuviTag> tags = new ArrayList<>();
        Log.d(TAG, from + " " + " found " + beacons.size());
        foundBeacon: for (Beacon beacon : beacons) {
            // the same tag can appear multiple times
            for (RuuviTag tag : tags) {
                if (tag.id.equals(beacon.getBluetoothAddress())) continue foundBeacon;
            }
            RuuviTag tag = fromAltbeacon(context, beacon);
            if (tag != null) {
                saveReading(tag);
                if (tag.favorite) tags.add(tag);
            }
        }
        if (tags.size() > 0 && gatewayOn) Http.post(tags, tagLocation, context);

        TagSensorReading.removeOlderThan(24);
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

    private void saveReading(RuuviTag ruuviTag) {
        RuuviTag dbTag = RuuviTag.get(ruuviTag.id);
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag);
            ruuviTag.update();
            if (!dbTag.favorite) return;
        } else {
            ruuviTag.updateAt = new Date();
            ruuviTag.save();
            return;
        }

        if (lastLogged == null) lastLogged = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -Constants.DATA_LOG_INTERVAL);
        long loggingThreshold = calendar.getTime().getTime();
        for (Map.Entry<String, Long> entry : lastLogged.entrySet())
        {
            if (entry.getKey().equals(ruuviTag.id) && entry.getValue() > loggingThreshold) {
                return;
            }
        }

        lastLogged.put(ruuviTag.id, new Date().getTime());
        TagSensorReading reading = new TagSensorReading(ruuviTag);
        reading.save();
        AlarmChecker.check(ruuviTag, context);
    }
}
