package com.ruuvi.station.model;

import android.content.Context;

import com.ruuvi.station.bluetooth.domain.IRuuviTag;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Created by berg on 18/09/17.
 */

public class ScanEventSingle extends Event {
    public IRuuviTag tag;

    public ScanEventSingle(Context context) {
        super(context);
    }
}
