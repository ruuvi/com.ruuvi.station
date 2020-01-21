package com.ruuvi.station.model;

import android.content.Context;

import com.ruuvi.station.bluetooth.domain.IRuuviTag;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Created by ISOHAJA on 14.7.2017.
 */

public class ScanEvent extends Event {
    public ArrayList<IRuuviTag> tags = new ArrayList<>();

    public ScanEvent(Context context) {
        super(context);
    }
}


