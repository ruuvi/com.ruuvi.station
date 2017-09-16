package com.ruuvi.tag.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.tag.database.LocalDatabase;

import java.util.Date;
import java.util.List;

/**
 * Created by elias on 15.9.2017.
 */

@Table(database = LocalDatabase.class)
public class TagSensorReading extends BaseModel {
    @PrimaryKey(autoincrement = true)
    @Column
    public int id;
    @Column
    public String ruuviTagId;
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
    @Column
    public double accelX;
    @Column
    public double accelY;
    @Column
    public double accelZ;
    @Column
    public double voltage;

    public TagSensorReading() {
    }

    public TagSensorReading(RuuviTag tag) {
        this.ruuviTagId = tag.id;
        this.temperature = tag.temperature;
        this.humidity = tag.humidity;
        this.pressure = tag.pressure;
        this.rssi = tag.rssi;
        this.accelX = tag.accelX;
        this.accelY = tag.accelY;
        this.accelZ = tag.accelZ;
        this.voltage = tag.voltage;
        this.createdAt = new Date();
    }

    public static List<TagSensorReading> getForTag(String id) {
        return SQLite.select()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(id))
                .queryList();
    }
}
