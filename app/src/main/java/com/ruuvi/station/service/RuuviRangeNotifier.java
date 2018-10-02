package com.ruuvi.station.service;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.ruuvi.station.gateway.Http;
import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RuuviRangeNotifier implements RangeNotifier {
    private static final String TAG = "RuuviRangeNotifier";
    private String from;
    private Context context;
    private Location tagLocation;

    public RuuviRangeNotifier(Context context, String from) {
        this.context = context;
        this.from = from;
    }

    private void updateLocation() {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        updateLocation();
        List<RuuviTag> tags = new ArrayList<>();
        Log.d(TAG, from + " " + " found " + beacons.size());
        foundBeacon: for (Beacon beacon : beacons) {
            // the same tag can appear multiple times
            for (RuuviTag tag : tags) {
                if (tag.id.equals(beacon.getBluetoothAddress())) continue foundBeacon;
            }
            RuuviTag tag = LeScanResult.fromAltbeacon(beacon);
            if (tag != null) {
                tags.add(tag);
                ScannerService.logTag(tag, context, true);
            }
        }
        if (tags.size() > 0) Http.post(tags, tagLocation, context);
    }
}
