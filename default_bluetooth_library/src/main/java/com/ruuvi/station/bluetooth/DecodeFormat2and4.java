package com.ruuvi.station.bluetooth;

import java.math.BigDecimal;
import java.math.RoundingMode;

class DecodeFormat2and4 implements LeScanResult.RuuviTagDecoder {

    @Override
    public FoundRuuviTag decode(byte[] data, int offset) {
        int[] pData = new int[8];
        for (int i = 0; i < data.length && i < 8; i++)
            pData[i] = data[i] & 0xFF;

        FoundRuuviTag tag = new FoundRuuviTag();
        tag.setDataFormat(pData[0]);
        tag.setHumidity(((pData[1] & 0xFF)) / 2.0);
        double uTemp = (((pData[2] & 127) << 8) | pData[3]);
        double tempSign = (pData[2] >> 7) & 1;
        tag.setTemperature(tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0);
        tag.setPressure(((pData[4] << 8) + pData[5]) + 50000.0);
        tag.setPressure(tag.getPressure() != null ? tag.getPressure() : 0.0 / 100.00);

        tag.setTemperature(round(tag.getTemperature() != null ? tag.getTemperature() : 0.0));
        tag.setHumidity(round(tag.getHumidity() != null ? tag.getHumidity() : 0.0));
        tag.setPressure(round(tag.getPressure() != null ? tag.getPressure() : 0.0));
        return tag;
    }

    private static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}