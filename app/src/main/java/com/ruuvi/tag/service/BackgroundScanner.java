package com.ruuvi.tag.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by berg on 30/09/17.
 */

public class BackgroundScanner extends BroadcastReceiver {
    private static final String TAG = "BackgroundScanner";
    public static final int REQUEST_CODE = 9001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "I got it!");
    }
}
