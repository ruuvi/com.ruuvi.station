package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

import static com.ruuvi.station.util.Utils.round;

public class DecodeFormat5 implements RuuviTagDecoder {
    @Override
    public RuuviTag decode(byte[] data) {
        RuuviTag tag = new RuuviTag();
        tag.dataFormat = 5;
        tag.temperature = (data[8] << 8 | data[9] & 0xFF) / 200d;
        tag.humidity = ((data[10] & 0xFF) << 8 | data[11] & 0xFF) / 400d;
        tag.pressure = (double) ((data[12] & 0xFF) << 8 | data[13] & 0xFF) + 50000;
        tag.pressure /= 100.0;

        tag.accelX = (data[14] << 8 | data[15] & 0xFF) / 1000d;
        tag.accelY = (data[16] << 8 | data[17] & 0xFF) / 1000d;
        tag.accelZ = (data[18] << 8 | data[19] & 0xFF) / 1000d;

        int powerInfo = (data[20] & 0xFF) << 8 | data[21] & 0xFF;
        if ((powerInfo >>> 5) != 0b11111111111) {
            tag.voltage = (powerInfo >>> 5) / 1000d + 1.6d;
        }
        if ((powerInfo & 0b11111) != 0b11111) {
            tag.txPower = (powerInfo & 0b11111) * 2 - 40;
        }
        tag.movementCounter = data[22] & 0xFF;
        tag.measurementSequenceNumber = (data[24] & 0xFF) << 8 | data[23] & 0xFF;

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
