package com.ruuvi.station.bluetooth;

import com.ruuvi.station.bluetooth.interfaces.IRuuviTag;
import com.ruuvi.station.bluetooth.interfaces.RuuviTagDecoder;
import com.ruuvi.station.bluetooth.interfaces.RuuviTagFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

class DecodeFormat3 implements RuuviTagDecoder {

    @Override
    public IRuuviTag decode(RuuviTagFactory factory, byte[] data, int offset) {
        IRuuviTag tag = factory.createTag();
        tag.setDataFormat(3);
        tag.setHumidity(((float) (data[1 + offset] & 0xFF)) / 2f);

        int temperatureSign = (data[2 + offset] >> 7) & 1;
        int temperatureBase = (data[2 + offset] & 0x7F);
        float temperatureFraction = ((float) data[3 + offset]) / 100f;
        tag.setTemperature(((float) temperatureBase) + temperatureFraction);
        if (temperatureSign == 1) {
            tag.setTemperature(tag.getTemperature() * -1);
        }

        int pressureHi = data[4 + offset] & 0xFF;
        int pressureLo = data[5 + offset] & 0xFF;
        tag.setPressure(pressureHi * 256 + 50000 + pressureLo);
        tag.setPressure(tag.getPressure() / 100.0);

        tag.setAccelX((data[6 + offset] << 8 | data[7 + offset] & 0xFF) / 1000f);
        tag.setAccelY((data[8 + offset] << 8 | data[9 + offset] & 0xFF) / 1000f);
        tag.setAccelZ((data[10 + offset] << 8 | data[11 + offset] & 0xFF) / 1000f);

        int battHi = data[12 + offset] & 0xFF;
        int battLo = data[13 + offset] & 0xFF;
        tag.setVoltage((battHi * 256 + battLo) / 1000f);

        // make it pretty
        tag.setTemperature(round(tag.getTemperature(), 2));
        tag.setHumidity(round(tag.getHumidity(), 2));
        tag.setPressure(round(tag.getPressure(), 2));
        tag.setVoltage(round(tag.getVoltage(), 4));
        tag.setAccelX(round(tag.getAccelX(), 4));
        tag.setAccelY(round(tag.getAccelY(), 4));
        tag.setAccelZ(round(tag.getAccelZ(), 4));
        return tag;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
