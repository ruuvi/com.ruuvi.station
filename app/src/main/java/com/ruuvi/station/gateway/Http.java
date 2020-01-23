package com.ruuvi.station.gateway;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.ruuvi.station.bluetooth.interfaces.IRuuviTag;
import com.ruuvi.station.database.RuuviTagRepository;
import com.ruuvi.station.model.RuuviTagEntity;
import com.ruuvi.station.model.ScanEvent;
import com.ruuvi.station.model.ScanEventSingle;
import com.ruuvi.station.model.ScanLocation;
import com.ruuvi.station.util.Preferences;

import java.util.List;

public class Http {
    private static final String TAG = "Http";

    public static void post(List<RuuviTagEntity> tags, Location location, Context context){
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
            IRuuviTag tagFromDb = RuuviTagRepository.get(tags.get(i).getId());
            // don't send data about tags not in the list
            if (tagFromDb == null || !tagFromDb.getFavorite()) continue;

            eventBatch.tags.add(tagFromDb);

            if (tagFromDb.getGatewayUrl() != null && !tagFromDb.getGatewayUrl().isEmpty()) {
                // send the single tag to its gateway
                ScanEventSingle single = new ScanEventSingle(context);
                single.location = scanLocation;
                single.tag = tagFromDb;

                Ion.with(context)
                        .load(tagFromDb.getGatewayUrl())
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

        Preferences prefs = new Preferences(context);
        String backendUrl = prefs.getGatewayUrl();

        if (!backendUrl.isEmpty() && eventBatch.tags.size() > 0)
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
