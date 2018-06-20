package com.ruuvi.station.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.database.FlowCursor;
import com.ruuvi.station.database.LocalDatabase;

import java.text.SimpleDateFormat;
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
    @Column
    public int dataFormat;
    @Column
    public double txPower;
    @Column
    public int movementCounter;
    @Column
    public int measurementSequenceNumber;

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
        this.dataFormat = tag.dataFormat;
        this.txPower = tag.txPower;
        this.movementCounter = tag.movementCounter;
        this.measurementSequenceNumber = tag.measurementSequenceNumber;
        this.createdAt = new Date();
    }

    public static List<TagSensorReading> getForTag(String id) {
        Date from = new Date();
        from.setHours(-24);
        return SQLite.select()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(id))
                .and(TagSensorReading_Table.createdAt.greaterThan(from))
                .queryList();
    }

    public static List<TagSensorReading> getLatestForTag(String id, int limit) {
        return SQLite.select()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(id))
                .orderBy(TagSensorReading_Table.id, false)
                .limit(limit)
                .queryList();
    }

    public static FlowCursor getMinAndMaxForTag(String id) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Date d = new Date();
        d.setHours(00);
        d.setMinutes(00);
        Long today = d.getTime();

        final String mQuery = "select ruuviTagId, min(temperature) as min, max(temperature) as max from TagSensorReading where ruuviTagId = ? and createdAt >= ? group by strftime('%d-%m-%Y', createdAt / 1000, 'unixepoch')";
        return FlowManager.getDatabaseForTable(TagSensorReading.class).getWritableDatabase().rawQuery(mQuery, new String[]{id, today.toString()});
    }

    public static FlowCursor getTempHistory(String id) {
        final String mQuery = "select strftime('%d.%m.%Y', createdAt / 1000, 'unixepoch') as createdAt, ruuviTagId, min(temperature) as min, max(temperature) as max from TagSensorReading where ruuviTagId = ? group by strftime('%d-%m-%Y', createdAt / 1000, 'unixepoch') order by createdAt desc limit 30";

        return FlowManager.getDatabaseForTable(TagSensorReading.class).getWritableDatabase().rawQuery(mQuery, new String[]{id});
    }
}
