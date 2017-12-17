package com.ruuvi.station.scanning;

import com.ruuvi.station.model.RuuviTag;

public interface RuuviTagListener {
    void tagFound(RuuviTag tag);
}
