package com.ruuvi.tag.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.tag.database.LocalDatabase;

import java.util.Date;

/**
 * Created by elias on 15.9.2017.
 */

@Table(database = LocalDatabase.class)
public class TagSensorReading extends BaseModel {
    @PrimaryKey(autoincrement = true)
    @Column
    public int id;
    @Column
    public String tagId;
    @Column
    public Date createdAt;
    @Column
    public double temperature;
    @Column
    public double humidity;
    @Column
    public double pressure;
    @Column
    public int rssi;

    public TagSensorReading() {
    }

    public TagSensorReading(String tagId, double temperature, double humidity, double pressure, int rssi) {
        this.tagId = tagId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        this.rssi = rssi;
        this.createdAt = new Date();
    }

    public TagSensorReading(RuuviTag tag) {
        this.tagId = tag.id;
        this.temperature = tag.temperature;
        this.humidity = tag.humidity;
        this.pressure = tag.pressure;
        this.rssi = tag.rssi;
        this.createdAt = new Date();
    }
}
