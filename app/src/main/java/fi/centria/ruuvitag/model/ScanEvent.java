package fi.centria.ruuvitag.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import fi.centria.ruuvitag.util.Ruuvitag;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class ScanEvent
{
    Date time;
    String deviceId;
    ArrayList<Ruuvitag> tags;
    String eventId;

    public ScanEvent(Context c, String deviceId)
    {
        time = new GregorianCalendar().getTime();
        this.deviceId = deviceId;
        tags = new ArrayList<>();
        eventId = UUID.randomUUID().toString();
    }

    public void addRuuvitag(Ruuvitag t)
    {
        tags.add(t);
    }

    public Date getDate() {
        return time;
    }

    public Ruuvitag getData(String tagId)
    {
        for(int i = 0; i < tags.size(); i++)
        {
            if(tags.get(i).getId().equalsIgnoreCase(tagId))
            return tags.get(i);
        }
        return null;
    }
}
