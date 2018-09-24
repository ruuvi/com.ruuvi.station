package com.ruuvi.station;

import android.app.Application;
import android.os.Build;

import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * Created by io53 on 10/09/17.
 */

public class RuuviScannerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(this);
    }
}
