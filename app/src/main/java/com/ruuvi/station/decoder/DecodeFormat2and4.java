package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

import static com.ruuvi.station.util.Utils.round;

public class DecodeFormat2and4 implements RuuviTagDecoder {
    @Override
    public RuuviTag decode(byte[] data, int offset) {
        int pData[] = new int[8];
        for (int i = 0; i < data.length && i < 8; i++)
            pData[i] = data[i] & 0xFF;

        RuuviTag tag = new RuuviTag();
        tag.setDataFormat(pData[0]);
        tag.setHumidity(((float) (pData[1] & 0xFF)) / 2f);
        double uTemp = (((pData[2] & 127) << 8) | pData[3]);
        double tempSign = (pData[2] >> 7) & 1;
        tag.setTemperature(tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0);
        tag.setPressure(((pData[4] << 8) + pData[5]) + 50000);
        tag.setPressure(tag.getPressure() / 100.00);

        tag.setTemperature(round(tag.getTemperature(), 2));
        tag.setHumidity(round(tag.getHumidity(), 2));
        tag.setPressure(round(tag.getPressure(), 2));
        return tag;
    }
}
