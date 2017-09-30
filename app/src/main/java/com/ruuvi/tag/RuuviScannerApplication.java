package com.ruuvi.tag;

import android.app.Application;
import android.os.Build;

import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * Created by berg on 10/09/17.
 */

public class RuuviScannerApplication extends Application {
    private static final boolean USE_NEW_API = true;

    public static boolean useNewApi() {
        return USE_NEW_API && Build.VERSION.SDK_INT >= 21;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(this);
    }
}
