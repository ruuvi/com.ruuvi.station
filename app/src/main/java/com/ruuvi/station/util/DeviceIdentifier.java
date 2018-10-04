package com.ruuvi.station.util;

import android.content.Context;

import java.util.UUID;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class DeviceIdentifier
{
    private static String uniqueID = null;

    public static String id(Context context)
    {
        Preferences prefs = new Preferences(context);
        uniqueID = prefs.getDeviceId();
        if (uniqueID.isEmpty())
        {
            uniqueID = UUID.randomUUID().toString();
            prefs.setDeviceId(uniqueID);
        }

        return uniqueID;
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
