package com.ruuvi.station.bluetooth;

import java.math.BigDecimal;
import java.math.RoundingMode;

class DecodeFormat3 implements LeScanResult.RuuviTagDecoder {

    @Override
    public FoundRuuviTag decode(byte[] data, int offset) {
        FoundRuuviTag tag = new FoundRuuviTag();
        tag.setDataFormat(3);
        tag.setHumidity(((float) (data[1 + offset] & 0xFF)) / 2.0);

        double temperatureSign = (data[2 + offset] >> 7) & 1;
        double temperatureBase = (data[2 + offset] & 0x7F);
        double temperatureFraction = (data[3 + offset]) / 100.0;
        tag.setTemperature(temperatureBase + temperatureFraction);
        if (temperatureSign == 1) {
            tag.setTemperature(tag.getTemperature() != null ? tag.getTemperature() : 0.0 * -1);
        }

        double pressureHi = data[4 + offset] & 0xFF;
        double pressureLo = data[5 + offset] & 0xFF;
        tag.setPressure(pressureHi * 256 + 50000 + pressureLo);
        tag.setPressure(tag.getPressure() != null ? tag.getPressure() : 0.0 / 100.0);

        tag.setAccelX((data[6 + offset] << 8 | data[7 + offset] & 0xFF) / 1000.0);
        tag.setAccelY((data[8 + offset] << 8 | data[9 + offset] & 0xFF) / 1000.0);
        tag.setAccelZ((data[10 + offset] << 8 | data[11 + offset] & 0xFF) / 1000.0);

        int battHi = data[12 + offset] & 0xFF;
        int battLo = data[13 + offset] & 0xFF;
        tag.setVoltage((battHi * 256 + battLo) / 1000.0);

        // make it pretty
        tag.setTemperature(round(tag.getTemperature() != null ? tag.getTemperature() : 0.0, 2));
        tag.setHumidity(round(tag.getHumidity() != null ? tag.getHumidity() : 0.0, 2));
        tag.setPressure(round(tag.getPressure(), 2));
        tag.setVoltage(round(tag.getVoltage() != null ? tag.getVoltage() : 0.0, 4));
        tag.setAccelX(round(tag.getAccelX() != null ? tag.getAccelX() : 0.0, 4));
        tag.setAccelY(round(tag.getAccelY() != null ? tag.getAccelY() : 0.0, 4));
        tag.setAccelZ(round(tag.getAccelZ() != null ? tag.getAccelZ() : 0.0, 4));
        return tag;
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
