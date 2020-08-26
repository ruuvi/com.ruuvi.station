package com.ruuvi.station.database.tables;

import android.annotation.SuppressLint;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.IndexGroup;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.queriable.StringQuery;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.database.LocalDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by elias on 15.9.2017.
 */

@Table(
        database = LocalDatabase.class,
        indexGroups = { @IndexGroup(number = 1, name = "TagId")}
        )
public class TagSensorReading extends BaseModel {
    @PrimaryKey(autoincrement = true)
    @Column
    public int id;
    @Index(indexGroups = 1)
    @Column
    public String ruuviTagId;
    @Index(indexGroups = 1)
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
    @Column
    public double humidityOffset;

    public TagSensorReading() {
    }

    public TagSensorReading(RuuviTagEntity tag) {
        this.ruuviTagId = tag.getId();
        this.temperature = tag.getTemperature();
        this.humidity = tag.getHumidity();
        this.humidityOffset = tag.getHumidityOffset();
        this.pressure = tag.getPressure();
        this.rssi = tag.getRssi();
        this.accelX = tag.getAccelX();
        this.accelY = tag.getAccelY();
        this.accelZ = tag.getAccelZ();
        this.voltage = tag.getVoltage();
        this.dataFormat = tag.getDataFormat();
        this.txPower = tag.getTxPower();
        this.movementCounter = tag.getMovementCounter();
        this.measurementSequenceNumber = tag.getMeasurementSequenceNumber();
        this.createdAt = new Date();
    }

    public static List<TagSensorReading> getForTag(String id) {
        return getForTag(id, 24);
    }

    public static List<TagSensorReading> getForTag(String id, int period) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -period);
        return SQLite.select()
                .from(TagSensorReading.class)
                .indexedBy(TagSensorReading_Table.index_TagId)
                .where(TagSensorReading_Table.ruuviTagId.eq(id))
                .and(TagSensorReading_Table.createdAt.greaterThan(cal.getTime()))
                .orderBy(TagSensorReading_Table.createdAt, true)
                .queryList();
    }

    public static List<TagSensorReading> getForTagPruned(String id, Integer interval, Integer period) {
        Calendar fromDate = Calendar.getInstance();
        fromDate.setTime(new Date());
        fromDate.add(Calendar.HOUR, -period);
        Integer pruningInterval = 1000 * 60 * interval;

        // seems like setSelectionArgs not working for StringQuery so we have to use String.Format instead
        @SuppressLint("DefaultLocale")
        String sqlString = String.format("Select tr.* from " +
                        "(select min(id) as id from TagSensorReading where RuuviTagId = '%s' and createdAt > %d group by createdAt / %d ) gr " +
                        "join TagSensorReading tr on gr.id = tr.id",
                id, fromDate.getTimeInMillis(), pruningInterval);

        StringQuery<TagSensorReading> query = new StringQuery<>(TagSensorReading.class, sqlString);
        return query.queryList();
    }

    public static List<TagSensorReading> getLatestForTag(String id, int limit) {
        return SQLite.select()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(id))
                .orderBy(TagSensorReading_Table.id, false)
                .limit(limit)
                .queryList();
    }

    public static void removeOlderThan(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -hours);
        SQLite.delete()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.createdAt.lessThan(cal.getTime()))
                .async()
                .execute();
    }

    public static void removeForTag(String id) {
        SQLite.delete()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(id))
                .async()
                .execute();
    }

    public static long countAll() {
        return SQLite.selectCountOf().from(TagSensorReading.class).count();
    }

    @Override
    public String toString() {
        return "TagSensorReading{" +
                "id=" + id +
                ", ruuviTagId='" + ruuviTagId + '\'' +
                ", createdAt=" + createdAt +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", humidityOffset=" + humidityOffset +
                ", pressure=" + pressure +
                ", rssi=" + rssi +
                ", accelX=" + accelX +
                ", accelY=" + accelY +
                ", accelZ=" + accelZ +
                ", voltage=" + voltage +
                ", dataFormat=" + dataFormat +
                ", txPower=" + txPower +
                ", movementCounter=" + movementCounter +
                ", measurementSequenceNumber=" + measurementSequenceNumber +
                '}';
    }
}
