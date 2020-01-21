package com.ruuvi.station.model;

import android.content.Context;

import com.ruuvi.station.bluetooth.interfaces.IRuuviTag;

/**
 * Created by berg on 18/09/17.
 */

public class ScanEventSingle extends Event {
    public IRuuviTag tag;

    public ScanEventSingle(Context context) {
        super(context);
    }
}
