package com.ruuvi.station.model;

import android.content.Context;

import com.ruuvi.station.bluetooth.interfaces.IRuuviTag;

import java.util.ArrayList;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class ScanEvent extends Event {
    public ArrayList<IRuuviTag> tags = new ArrayList<>();

    public ScanEvent(Context context) {
        super(context);
    }
}


