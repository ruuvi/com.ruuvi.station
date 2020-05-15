package com.ruuvi.station.gateway.data;

import android.content.Context;

import com.ruuvi.station.model.Event;
import com.ruuvi.station.database.tables.RuuviTagEntity;

/**
 * Created by berg on 18/09/17.
 */

public class ScanEventSingle extends Event {
    public RuuviTagEntity tag;

    public ScanEventSingle(Context context) {
        super(context);
    }
}
