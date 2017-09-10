package fi.ruuvi.android;

import android.app.Application;

import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * Created by berg on 10/09/17.
 */

public class RuuviScannerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(this);
    }
}
