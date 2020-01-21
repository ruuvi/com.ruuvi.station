package com.ruuvi.station.bluetooth;

import com.ruuvi.station.bluetooth.domain.IRuuviTag;

import java.math.BigDecimal;
import java.math.RoundingMode;

class DecodeFormat2and4 implements RuuviTagDecoder {

    @Override
    public IRuuviTag decode(RuuviTagFactory factory, byte[] data, int offset) {
        int pData[] = new int[8];
        for (int i = 0; i < data.length && i < 8; i++)
            pData[i] = data[i] & 0xFF;

        IRuuviTag tag = factory.createTag();
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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
