package com.ruuvi.station.model;

import android.content.Context;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.data.Blob;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.R;
import com.ruuvi.station.bluetooth.domain.IRuuviTag;
import com.ruuvi.station.database.LocalDatabase;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.Utils;

import java.util.Date;
import java.util.List;


/**
 * Created by tmakinen on 15.6.2017.
 */

@Table(database = LocalDatabase.class)
public class RuuviTag extends BaseModel implements IRuuviTag {
    @Column
    @PrimaryKey
    private String id;
    @Column
    private String url;
    @Column
    private int rssi;
    private double[] data;
    @Column
    private String name;
    @Column
    private double temperature;
    @Column
    private double humidity;
    @Column
    private double pressure;
    @Column
    private boolean favorite;
    @Column
    public Blob rawDataBlob;
    private byte[] rawData;
    @Column
    private double accelX;
    @Column
    private double accelY;
    @Column
    private double accelZ;
    @Column
    private double voltage;
    @Column
    private Date updateAt;
    @Column
    private String gatewayUrl;
    @Column
    private int defaultBackground;
    @Column
    private String userBackground;
    @Column
    private int dataFormat;
    @Column
    private double txPower;
    @Column
    private int movementCounter;
    @Column
    private int measurementSequenceNumber;

    public RuuviTag() {
    }

    public RuuviTag preserveData(RuuviTag tag) {
        tag.setName(this.getName());
        tag.setFavorite(this.isFavorite());
        tag.setGatewayUrl(this.getGatewayUrl());
        tag.setDefaultBackground(this.getDefaultBackground());
        tag.setUserBackground(this.getUserBackground());
        tag.setUpdateAt(new Date());
        return tag;
    }

    private double getFahrenheit() {
        return Utils.celciusToFahrenheit(this.getTemperature());
    }

    public static String getTemperatureUnit(Context context) {
        return new Preferences(context).getTemperatureUnit();
    }

    public String getTemperatureString(Context context) {
        String temperatureUnit = RuuviTag.getTemperatureUnit(context);
        if (temperatureUnit.equals("C")) {
            return String.format(context.getString(R.string.temperature_reading), this.getTemperature()) + temperatureUnit;
        }
        return String.format(context.getString(R.string.temperature_reading), this.getFahrenheit()) + temperatureUnit;
    }

    public String getDispayName() {
        return (this.getName() != null && !this.getName().isEmpty()) ? this.getName() : this.getId();
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
                .where(Alarm_Table.ruuviTagId.eq(this.getId()))
                .execute();
        SQLite.delete()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(this.getId()))
                .execute();

        this.delete();
    }


    @org.jetbrains.annotations.Nullable
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int getRssi() {
        return rssi;
    }

    @Override
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public double[] getData() {
        return data;
    }

    @Override
    public void setData(double[] data) {
        this.data = data;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public double getHumidity() {
        return humidity;
    }

    @Override
    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    @Override
    public double getPressure() {
        return pressure;
    }

    @Override
    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public boolean isFavorite() {
        return favorite;
    }

    @Override
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public Object getRawDataBlob() {
        return rawDataBlob;
    }

    @Override
    public void setRawDataBlob(Object rawDataBlob) {
        this.rawDataBlob = (Blob) rawDataBlob;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    @Override
    public double getAccelX() {
        return accelX;
    }

    @Override
    public void setAccelX(double accelX) {
        this.accelX = accelX;
    }

    @Override
    public double getAccelY() {
        return accelY;
    }

    @Override
    public void setAccelY(double accelY) {
        this.accelY = accelY;
    }

    @Override
    public double getAccelZ() {
        return accelZ;
    }

    @Override
    public void setAccelZ(double accelZ) {
        this.accelZ = accelZ;
    }

    @Override
    public double getVoltage() {
        return voltage;
    }

    @Override
    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public Date getUpdateAt() {
        return updateAt;
    }

    @Override
    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public String getGatewayUrl() {
        return gatewayUrl;
    }

    @Override
    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    @Override
    public int getDefaultBackground() {
        return defaultBackground;
    }

    @Override
    public void setDefaultBackground(int defaultBackground) {
        this.defaultBackground = defaultBackground;
    }

    @Override
    public int getDataFormat() {
        return dataFormat;
    }

    @Override
    public void setDataFormat(int dataFormat) {
        this.dataFormat = dataFormat;
    }

    @Override
    public double getTxPower() {
        return txPower;
    }

    @Override
    public void setTxPower(double txPower) {
        this.txPower = txPower;
    }

    @Override
    public int getMovementCounter() {
        return movementCounter;
    }

    @Override
    public void setMovementCounter(int movementCounter) {
        this.movementCounter = movementCounter;
    }

    @Override
    public int getMeasurementSequenceNumber() {
        return measurementSequenceNumber;
    }

    @Override
    public void setMeasurementSequenceNumber(int measurementSequenceNumber) {
        this.measurementSequenceNumber = measurementSequenceNumber;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public String getUserBackground() {
        return userBackground;
    }

    @Override
    public void setUserBackground(String userBackground) {
        this.userBackground = userBackground;
    }

    @Override
    public boolean getFavorite() {
        return favorite;
    }
}