package com.ruuvi.station.model;

import android.content.Context;

import java.util.Date;
import java.util.List;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.data.Blob;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.R;
import com.ruuvi.station.database.LocalDatabase;
import com.ruuvi.station.util.Humidity;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.Utils;


/**
 * Created by tmakinen on 15.6.2017.
 */

@Table(database = LocalDatabase.class)
public class RuuviTag extends BaseModel {
    @Column
    @PrimaryKey
    public String id;
    @Column
    public String url;
    @Column
    public int rssi;
    public double[] data;
    @Column
    public String name;
    @Column
    public double temperature;
    @Column
    public double humidity;
    @Column
    public double pressure;
    @Column
    public boolean favorite;
    @Column
    public Blob rawDataBlob;
    public byte[] rawData;
    @Column
    public double accelX;
    @Column
    public double accelY;
    @Column
    public double accelZ;
    @Column
    public double voltage;
    @Column
    public Date updateAt;
    @Column
    public String gatewayUrl;
    @Column
    public int defaultBackground;
    @Column
    public String userBackground;
    @Column
    public int dataFormat;
    @Column
    public double txPower;
    @Column
    public int movementCounter;
    @Column
    public int measurementSequenceNumber;

    public RuuviTag() {
    }

    public RuuviTag preserveData(RuuviTag tag) {
        tag.name = this.name;
        tag.favorite = this.favorite;
        tag.gatewayUrl = this.gatewayUrl;
        tag.defaultBackground = this.defaultBackground;
        tag.userBackground = this.userBackground;
        tag.updateAt = new Date();
        return tag;
    }

    private double getFahrenheit() {
        return Utils.celciusToFahrenheit(this.temperature);
    }

    private double getKelvin() {
        return Utils.celsiusToKelvin(this.temperature);
    }

    public static String getTemperatureUnit(Context context) {
        return new Preferences(context).getTemperatureUnit();
    }

    public static HumidityUnit getHumidityUnit(Context context) {
        return new Preferences(context).getHumidityUnit();
    }

    public String getTemperatureString(Context context) {
        String temperatureUnit = RuuviTag.getTemperatureUnit(context);
        if (temperatureUnit.equals("K")) {
            return String.format(context.getString(R.string.temperature_reading), this.getKelvin()) + temperatureUnit;
        } else if (temperatureUnit.equals("F")) {
            return String.format(context.getString(R.string.temperature_reading), this.getFahrenheit()) + "°" + temperatureUnit;
        } else {
            return String.format(context.getString(R.string.temperature_reading), this.temperature) + "°" + temperatureUnit;
        }
    }

    public String getHumidityString(Context context) {
        HumidityUnit humidityUnit = RuuviTag.getHumidityUnit(context);
        Humidity calculation = new Humidity(temperature, humidity / 100.0);
        switch (humidityUnit) {
            case PERCENT:
                return String.format(context.getString(R.string.humidity_reading), humidity);
            case GM3:
                return String.format(context.getString(R.string.humidity_absolute_reading), calculation.getAh());
            case DEW:
                String temperatureUnit = RuuviTag.getTemperatureUnit(context);
                if (temperatureUnit.equals("K")) {
                    return String.format(context.getString(R.string.humidity_dew_reading), calculation.getTdK()) + " " + temperatureUnit;
                } else if (temperatureUnit.equals("F")) {
                    return String.format(context.getString(R.string.humidity_dew_reading), calculation.getTdF()) + " °" + temperatureUnit;
                } else {
                    return String.format(context.getString(R.string.humidity_dew_reading), calculation.getTd()) + " °" + temperatureUnit;
                }
            default:
                return context.getString(R.string.n_a);
        }
    }

    public String getDispayName() {
        return (this.name != null && !this.name.isEmpty()) ? this.name : this.id;
    }

    public static List<RuuviTag> getAll(boolean favorite) {
        return SQLite.select()
                .from(RuuviTag.class)
                .where(RuuviTag_Table.favorite.eq(favorite))
                .queryList();
    }

    public static RuuviTag get(String id) {
        return SQLite.select()
                .from(RuuviTag.class)
                .where(RuuviTag_Table.id.eq(id))
                .querySingle();
    }

    public void deleteTagAndRelatives() {
        SQLite.delete()
                .from(Alarm.class)
                .where(Alarm_Table.ruuviTagId.eq(this.id))
                .execute();
        SQLite.delete()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(this.id))
                .execute();

        this.delete();
    }
}