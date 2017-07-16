package fi.centria.ruuvitag.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class DeviceIdentifier
{
    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "pref_device_id";

    public synchronized static String id(Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);


            uniqueID = settings.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null)
            {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }

        return uniqueID;
    }
}
