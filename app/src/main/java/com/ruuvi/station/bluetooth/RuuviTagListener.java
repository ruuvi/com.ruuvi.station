package com.ruuvi.station.bluetooth;

import com.ruuvi.station.bluetooth.domain.IRuuviTag;

public interface RuuviTagListener {

    void tagFound(IRuuviTag tag);

}
