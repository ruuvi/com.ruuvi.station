package com.ruuvi.station.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.database.LocalDatabase;

import java.util.Date;
import java.util.HashMap;

@Table(database = LocalDatabase.class)
public class HumidityCalibration extends BaseModel {
    public static HashMap<String, HumidityCalibration> cache = new HashMap<>();
    public static HumidityCalibration calibrate(RuuviTag tag) {
        HumidityCalibration prevCalibration = HumidityCalibration.get(tag);
        float prevCalibrationValue = 0f;
        if (prevCalibration != null) {
            prevCalibrationValue = prevCalibration.humidityOffset;
            prevCalibration.delete();
        }
        float calibration = 75f-((float) tag.getHumidity() -prevCalibrationValue);
        HumidityCalibration newCalibration = new HumidityCalibration();
        newCalibration.humidityOffset = calibration;
        newCalibration.mac = tag.getId();
        cache.put(tag.getId(), newCalibration);
        return newCalibration;
    }
    public static void clear(RuuviTag tag) {
        HumidityCalibration calibration = HumidityCalibration.get(tag);
        if (calibration != null) {
            calibration.delete();
        }
        cache.remove(tag.getId());
    }
    public static RuuviTag apply(RuuviTag tag) {
        HumidityCalibration calibration = get(tag);
        if (calibration != null) {
            tag.setHumidity(tag.getHumidity() + calibration.humidityOffset);
        }
        return tag;
    }
    public static HumidityCalibration get(RuuviTag tag) {
        if (cache.containsKey(tag.getId())) {
            return cache.get(tag.getId());
        }
        return SQLite.select()
                .from(HumidityCalibration.class)
                .where(HumidityCalibration_Table.mac.eq(tag.getId()))
                .querySingle();
    }
    public HumidityCalibration() {
    }
    @Column
    @PrimaryKey
    public String mac;
    @Column
    public float humidityOffset;
    @Column
    public Date timestamp = new Date();
}
