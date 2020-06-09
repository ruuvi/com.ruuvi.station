package com.ruuvi.station.model;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import com.ruuvi.station.gateway.data.ScanLocation;
import com.ruuvi.station.util.DeviceIdentifier;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import timber.log.Timber;

import static android.content.Context.BATTERY_SERVICE;

public class Event {
    public Date time;
    public String deviceId;
    public String eventId;
    public ScanLocation location;
    public int batteryLevel;

    public Event(Context context) {
        this.deviceId = DeviceIdentifier.id(context);
        this.time = new GregorianCalendar().getTime();
        this.eventId = UUID.randomUUID().toString();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                BatteryManager bm = (BatteryManager)context.getSystemService(BATTERY_SERVICE);
                this.batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            } catch (Exception e) {
                Timber.tag("TEST").e(e);
            }
        }
    }
}
