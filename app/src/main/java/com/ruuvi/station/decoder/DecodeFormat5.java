package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

import static com.ruuvi.station.util.Utils.round;

public class DecodeFormat5 implements RuuviTagDecoder {
    // offset = 7
    @Override
    public RuuviTag decode(byte[] data, int offset) {
        RuuviTag tag = new RuuviTag();
        tag.dataFormat = 5;
        tag.temperature = (data[1 + offset] << 8 | data[2 + offset] & 0xFF) / 200d;
        tag.humidity = ((data[3 + offset] & 0xFF) << 8 | data[4 + offset] & 0xFF) / 400d;
        tag.pressure = (double) ((data[5 + offset] & 0xFF) << 8 | data[6 + offset] & 0xFF) + 50000;
        tag.pressure /= 100.0;

        tag.accelX = (data[7 + offset] << 8 | data[8 + offset] & 0xFF) / 1000d;
        tag.accelY = (data[9 + offset] << 8 | data[10 + offset] & 0xFF) / 1000d;
        tag.accelZ = (data[11 + offset] << 8 | data[12 + offset] & 0xFF) / 1000d;

        int powerInfo = (data[13 + offset] & 0xFF) << 8 | data[14 + offset] & 0xFF;
        if ((powerInfo >>> 5) != 0b11111111111) {
            tag.voltage = (powerInfo >>> 5) / 1000d + 1.6d;
        }
        if ((powerInfo & 0b11111) != 0b11111) {
            tag.txPower = (powerInfo & 0b11111) * 2 - 40;
        }
        tag.movementCounter = data[15 + offset] & 0xFF;
        tag.measurementSequenceNumber = (data[16 + offset] & 0xFF) << 8 | data[17 + offset] & 0xFF;

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
