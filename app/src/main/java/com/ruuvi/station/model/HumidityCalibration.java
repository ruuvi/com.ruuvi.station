package com.ruuvi.station.model;

import android.support.annotation.Nullable;

import com.ruuvi.station.database.tables.RuuviTagEntity;

import java.util.Date;
import java.util.HashMap;

public class HumidityCalibration {

    private static HashMap<String, HumidityCalibration> cache = new HashMap<>();

    public String mac;
    public double humidityOffset;
    public Date timestamp = new Date();

    public static HumidityCalibration calibrate(RuuviTagEntity tag) {
        HumidityCalibration prevCalibration = HumidityCalibration.get(tag);
        double prevCalibrationValue = 0f;
        if (prevCalibration != null) {
            prevCalibrationValue = prevCalibration.humidityOffset;
        }
        double calibration = 75f - (tag.getHumidity() != null ? tag.getHumidity() : 0.0 - prevCalibrationValue);
        HumidityCalibration newCalibration = new HumidityCalibration();
        newCalibration.humidityOffset = calibration;
        newCalibration.mac = tag.getId();
        cache.put(tag.getId(), newCalibration);
        return newCalibration;
    }

    public static void clear(RuuviTagEntity tag) {
        cache.remove(tag.getId());
    }

    public static RuuviTagEntity apply(RuuviTagEntity tag) {
        HumidityCalibration calibration = get(tag);
        if (calibration != null) {
            tag.setHumidity(tag.getHumidity() + calibration.humidityOffset);
        }
        return tag;
    }

    @Nullable
    public static HumidityCalibration get(RuuviTagEntity tag) {
        if (cache.containsKey(tag.getId())) {
            return cache.get(tag.getId());
        }
        return null;
    }
}
