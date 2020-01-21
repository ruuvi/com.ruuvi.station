package com.ruuvi.station.bluetooth.interfaces;

public interface RuuviTagDecoder {

    IRuuviTag decode(RuuviTagFactory factory, byte[] data, int offset);

}
