package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

import static com.ruuvi.station.util.Utils.round;

public class DecodeFormat3 implements RuuviTagDecoder {
    @Override
    public RuuviTag decode(byte[] data, int offset) {
        RuuviTag tag = new RuuviTag();
        tag.dataFormat = 3;
        tag.humidity = ((float) (data[1 + offset] & 0xFF)) / 2f;

        int temperatureSign = (data[2 + offset] >> 7) & 1;
        int temperatureBase = (data[2 + offset] & 0x7F);
        float temperatureFraction = ((float) data[3 + offset]) / 100f;
        tag.temperature = ((float) temperatureBase) + temperatureFraction;
        if (temperatureSign == 1) {
            tag.temperature *= -1;
        }

        int pressureHi = data[4 + offset] & 0xFF;
        int pressureLo = data[5 + offset] & 0xFF;
        tag.pressure = pressureHi * 256 + 50000 + pressureLo;
        tag.pressure /= 100.0;

        tag.accelX = (data[6 + offset] << 8 | data[7 + offset] & 0xFF) / 1000f;
        tag.accelY = (data[8 + offset] << 8 | data[9 + offset] & 0xFF) / 1000f;
        tag.accelZ = (data[10 + offset] << 8 | data[11 + offset] & 0xFF) / 1000f;

        int battHi = data[12 + offset] & 0xFF;
        int battLo = data[13 + offset] & 0xFF;
        tag.voltage = (battHi * 256 + battLo) / 1000f;

        // make it pretty
        tag.temperature = round(tag.temperature, 2);
        tag.humidity = round(tag.humidity, 2);
        tag.pressure = round(tag.pressure, 2);
        tag.voltage = round(tag.voltage, 4);
        tag.accelX = round(tag.accelX, 4);
        tag.accelY = round(tag.accelY, 4);
        tag.accelZ = round(tag.accelZ, 4);
        return tag;
    }
}
