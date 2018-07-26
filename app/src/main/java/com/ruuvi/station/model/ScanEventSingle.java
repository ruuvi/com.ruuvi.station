package com.ruuvi.station.model;

import android.content.Context;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Created by berg on 18/09/17.
 */

public class ScanEventSingle {
    public Date time;
    public String deviceId;
    public RuuviTag tag;
    public String eventId;
    public ScanLocation location;

    public ScanEventSingle(String deviceId, Date time)
    {
        this.time = time;
        this.deviceId = deviceId;
        eventId = UUID.randomUUID().toString();
    }

    public ScanEventSingle(Context c, String deviceId) {
        time = new GregorianCalendar().getTime();
        this.deviceId = deviceId;
        eventId = UUID.randomUUID().toString();
    }
}
