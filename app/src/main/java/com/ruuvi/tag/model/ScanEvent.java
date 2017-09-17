package com.ruuvi.tag.model;

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
    ArrayList<RuuviTag> tags;
    public String eventId;

    public ScanEvent(String deviceId,Date time)
    {
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

    public void addRuuviTag(RuuviTag t) {
        tags.add(t);
    }

    public Date getDate() {
        return time;
    }

    public RuuviTag getData(String tagId) {
        for(int i = 0; i < tags.size(); i++) {
            if(tags.get(i).id.equalsIgnoreCase(tagId))
            return tags.get(i);
        }
        return null;
    }

    public int tagCount() {
        return tags.size();
    }

    public RuuviTag getDataFromIndex(int i) {
        return tags.get(i);
    }
}
