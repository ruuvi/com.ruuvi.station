package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

public interface RuuviTagDecoder {
    RuuviTag decode(byte[] data);
}
