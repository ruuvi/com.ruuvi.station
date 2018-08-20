package com.ruuvi.station.model;

import android.content.Context;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Created by berg on 18/09/17.
 */

public class ScanEventSingle extends Event {
    public RuuviTag tag;

    public ScanEventSingle(Context context) {
        super(context);
    }
}
