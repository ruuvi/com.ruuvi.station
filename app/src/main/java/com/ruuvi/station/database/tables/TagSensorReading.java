package com.ruuvi.station.database.tables;

import android.annotation.SuppressLint;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.IndexGroup;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.queriable.StringQuery;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.database.LocalDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by elias on 15.9.2017.
 */

@Table(
        database = LocalDatabase.class,
        indexGroups = {@IndexGroup(number = 1, name = "TagId")}
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

    public static void saveList(List<TagSensorReading> readings) {
        String insertQuery = "insert into TagSensorReading (`ruuviTagId`, `createdAt`, `temperature`, `humidity`, `pressure`, `rssi`, `accelX`, `accelY`, `accelZ`, `voltage`, `dataFormat`, `txPower`, `movementCounter`, `measurementSequenceNumber`, `humidityOffset`) values ";
        String valuesQuery = " (\"%s\",       %d,          %f,            %f,         %f,         %d,     %f,       %f,       %f,       %f,        %d,           %f,        %d,                %d,                          %f),";
        List<String> queries = new ArrayList<>();
        for (int i = 0; i < readings.size(); i++) {
            TagSensorReading reading = readings.get(i);
            queries.add(String.format(Locale.ENGLISH,
                    valuesQuery,
                    reading.ruuviTagId,
                    reading.createdAt.getTime(),
                    reading.temperature,
                    reading.humidity,
                    reading.pressure,
                    reading.rssi,
                    reading.accelX,
                    reading.accelY,
                    reading.accelZ,
                    reading.voltage,
                    reading.dataFormat,
                    reading.txPower,
                    reading.movementCounter,
                    reading.measurementSequenceNumber,
                    reading.humidityOffset));
        }

        int BATCH_SIZE = 100;
        for (int i = 0; i < queries.size(); ) {
            StringBuilder query = new StringBuilder(insertQuery);
            for (int j = 0; j < BATCH_SIZE; j++) {
                if (i + j >= queries.size()) {
                    i = i + j;
                    break;
                }
                query.append(queries.get(i + j));
                if (j >= BATCH_SIZE - 1) {
                    i = i + j;
                }
            }
            query.replace(query.length() - 1, query.length(), ";");
            FlowManager.getWritableDatabase(LocalDatabase.NAME).execSQL(query.toString());
        }
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
