package com.ruuvi.station.bluetooth;

import com.ruuvi.station.model.RuuviTag;

public interface RuuviTagListener {
    void tagFound(RuuviTag tag);
}
