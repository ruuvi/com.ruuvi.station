package com.ruuvi.station.decoder;

import com.ruuvi.station.model.RuuviTag;

import static com.ruuvi.station.util.Utils.round;

public class DecodeFormat5 implements RuuviTagDecoder {
    // offset = 7
    @Override
    public RuuviTag decode(byte[] data, int offset) {
        RuuviTag tag = new RuuviTag();
        tag.setDataFormat(5);
        tag.setTemperature((data[1 + offset] << 8 | data[2 + offset] & 0xFF) / 200d);
        tag.setHumidity(((data[3 + offset] & 0xFF) << 8 | data[4 + offset] & 0xFF) / 400d);
        tag.setPressure((double) ((data[5 + offset] & 0xFF) << 8 | data[6 + offset] & 0xFF) + 50000);
        tag.setPressure(tag.getPressure() / 100.0);

        tag.setAccelX((data[7 + offset] << 8 | data[8 + offset] & 0xFF) / 1000d);
        tag.setAccelY((data[9 + offset] << 8 | data[10 + offset] & 0xFF) / 1000d);
        tag.setAccelZ((data[11 + offset] << 8 | data[12 + offset] & 0xFF) / 1000d);

        int powerInfo = (data[13 + offset] & 0xFF) << 8 | data[14 + offset] & 0xFF;
        if ((powerInfo >>> 5) != 0b11111111111) {
            tag.setVoltage((powerInfo >>> 5) / 1000d + 1.6d);
        }
        if ((powerInfo & 0b11111) != 0b11111) {
            tag.setTxPower((powerInfo & 0b11111) * 2 - 40);
        }
        tag.setMovementCounter(data[15 + offset] & 0xFF);
        tag.setMeasurementSequenceNumber((data[17 + offset] & 0xFF) << 8 | data[16 + offset] & 0xFF);

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
}
