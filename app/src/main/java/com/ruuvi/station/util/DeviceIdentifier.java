package com.ruuvi.station.util;

import android.content.Context;

import com.ruuvi.station.app.preferences.Preferences;

import java.util.UUID;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class DeviceIdentifier {
    private static String uniqueID = null;

    public static String id(Context context) {
        Preferences prefs = new Preferences(context);
        uniqueID = prefs.getDeviceId();
        if (uniqueID.isEmpty()) {
            String result = Long.toString(UUID.randomUUID().getMostSignificantBits(), 36) +
                    Long.toString(UUID.randomUUID().getLeastSignificantBits(), 36);
            uniqueID = result.substring(1);
            prefs.setDeviceId(uniqueID);
        }

        return uniqueID;
    }

    public static String generateId() {
        String id = Long.toString(UUID.randomUUID().getMostSignificantBits(), 36) +
                Long.toString(UUID.randomUUID().getLeastSignificantBits(), 36);
        return id.substring(1);
    }
}
