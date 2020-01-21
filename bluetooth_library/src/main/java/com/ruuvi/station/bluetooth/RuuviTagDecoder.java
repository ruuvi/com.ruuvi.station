package com.ruuvi.station.bluetooth;

import com.ruuvi.station.bluetooth.RuuviTagFactory;
import com.ruuvi.station.bluetooth.domain.IRuuviTag;

public interface RuuviTagDecoder {

    IRuuviTag decode(RuuviTagFactory factory, byte[] data, int offset);

}
