package com.ruuvi.station.gateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.ScanEvent;
import com.ruuvi.station.model.ScanEventSingle;
import com.ruuvi.station.model.ScanLocation;
import com.ruuvi.station.util.DeviceIdentifier;

import java.util.Iterator;
import java.util.List;

public class Http {
    private static final String TAG = "Http";

    public static void post(List<RuuviTag> tags, Location location, Context context){
        String deviceId = DeviceIdentifier.id(context);

        ScanLocation scanLocation = null;
        if (location != null) {
            scanLocation = new ScanLocation();
            scanLocation.latitude = location.getLatitude();
            scanLocation.longitude = location.getLongitude();
            scanLocation.accuracy = location.getAccuracy();
        }


        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
        Ion.getDefault(context).configure().setGson(gson);

        ScanEvent eventBatch = new ScanEvent(context);
        eventBatch.location = scanLocation;
        for (int i = 0; i < tags.size(); i++) {
            RuuviTag tagFromDb = RuuviTag.get(tags.get(i).id);
            // don't send data about tags not in the list
            if (tagFromDb == null || !tagFromDb.favorite) continue;

            eventBatch.tags.add(tagFromDb);

            if (tagFromDb.gatewayUrl != null && !tagFromDb.gatewayUrl.isEmpty()) {
                // send the single tag to its gateway
                ScanEventSingle single = new ScanEventSingle(context);
                single.location = scanLocation;
                single.tag = tagFromDb;

                Ion.with(context)
                        .load(tagFromDb.gatewayUrl)
                        .setJsonPojoBody(single)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {
                                if (e != null) {
                                    Log.e(TAG, "Sending failed.");
                                }
                            }
                        });
            }
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String backendUrl = settings.getString("pref_backend", null);

        if (backendUrl != null && eventBatch.tags.size() > 0)
        {
            Ion.with(context)
                    .load(backendUrl)
                    .setJsonPojoBody(eventBatch)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (e != null) {
                                Log.e(TAG, "Batch sending failed.");
                            }
                        }
                    });
        }
    }
}
