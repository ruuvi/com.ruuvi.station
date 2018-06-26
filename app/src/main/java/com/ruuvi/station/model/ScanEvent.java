package com.ruuvi.station.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class ScanEvent {
    public Date time;
    public String deviceId;
    public ArrayList<RuuviTag> tags;
    public String eventId;
    public ScanLocation location;

    public ScanEvent(String deviceId,Date time) {
        this.time = time;
        this.deviceId = deviceId;
        eventId = UUID.randomUUID().toString();
        tags = new ArrayList<>();
    }

    public ScanEvent(Context c, String deviceId) {
        time = new GregorianCalendar().getTime();
        this.deviceId = deviceId;
        tags = new ArrayList<>();
        eventId = UUID.randomUUID().toString();
    }
}


