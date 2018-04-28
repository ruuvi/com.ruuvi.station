package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

import static com.ruuvi.station.util.Utils.round;

public class DecodeFormat2and4 implements RuuviTagDecoder {
    @Override
    public RuuviTag decode(byte[] data) {
        int pData[] = new int[8];
        for (int i = 0; i < data.length; i++)
            pData[i] = data[i] & 0xFF;

        RuuviTag tag = new RuuviTag();
        tag.dataFormat = pData[0];
        tag.humidity = ((float) (pData[1] & 0xFF)) / 2f;
        double uTemp = (((pData[2] & 127) << 8) | pData[3]);
        double tempSign = (pData[2] >> 7) & 1;
        tag.temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
        tag.pressure = ((pData[4] << 8) + pData[5]) + 50000;
        tag.pressure /= 100.00;

        tag.temperature = round(tag.temperature, 2);
        tag.humidity = round(tag.humidity, 2);
        tag.pressure = round(tag.pressure, 2);
        return tag;
    }
}
