package com.ruuvi.station.model;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class ScanEvent extends Event {
    public ArrayList<RuuviTagEntity> tags = new ArrayList<>();

    public ScanEvent(Context context) {
        super(context);
    }
}


