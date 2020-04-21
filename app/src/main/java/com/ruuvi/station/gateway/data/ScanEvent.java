package com.ruuvi.station.gateway.data;

import android.content.Context;

import com.ruuvi.station.model.Event;
import com.ruuvi.station.model.RuuviTagEntity;

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


