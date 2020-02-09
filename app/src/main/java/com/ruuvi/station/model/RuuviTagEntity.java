package com.ruuvi.station.model;

import android.support.annotation.Nullable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.bluetooth.FoundRuuviTag;
import com.ruuvi.station.database.LocalDatabase;

import java.util.Date;


// FIXME: change to the same database table name as was before the refactoring
@Table(name = "RuuviTag", database = LocalDatabase.class)
public class RuuviTagEntity extends BaseModel {

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

    public RuuviTagEntity() {
    }

    public RuuviTagEntity(FoundRuuviTag tag) {
        this.id = tag.getId();
        this.url = tag.getUrl();
        this.rssi = tag.getRssi() != null ? tag.getRssi() : 0;
        this.temperature = tag.getTemperature() != null ? tag.getTemperature() : 0.0;
        this.humidity = tag.getHumidity() != null ? tag.getHumidity() : 0.0;
        this.pressure = tag.getPressure() != null ? tag.getPressure() : 0.0;
        this.accelX = tag.getAccelX() != null ? tag.getAccelX() : 0.0;
        this.accelY = tag.getAccelY() != null ? tag.getAccelY() : 0.0;
        this.accelZ = tag.getAccelZ() != null ? tag.getAccelZ() : 0.0;
        this.voltage = tag.getVoltage() != null ? tag.getVoltage() : 0.0;
        this.dataFormat = tag.getDataFormat() != null ? tag.getDataFormat() : 0;
        this.txPower = tag.getTxPower() != null ? tag.getTxPower() : 0;
        this.movementCounter = tag.getMovementCounter() != null ? tag.getMovementCounter() : 0;
        this.measurementSequenceNumber = tag.getMeasurementSequenceNumber() != null
                ? tag.getMeasurementSequenceNumber() : 0;
    }

    public RuuviTagEntity preserveData(RuuviTagEntity tag) {
        tag.setName(this.getName());
        tag.setFavorite(this.isFavorite());
        tag.setGatewayUrl(this.getGatewayUrl());
        tag.setDefaultBackground(this.getDefaultBackground());
        tag.setUserBackground(this.getUserBackground());
        tag.setUpdateAt(new Date());
        return tag;
    }

    public String getDisplayName() {
        return (this.getName() != null && !this.getName().isEmpty()) ? this.getName() : this.getId();
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public String getId() {
        return id;
    }

    //@Override
    public void setId(String id) {
        this.id = id;
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public String getUrl() {
        return url;
    }

    //@Override
    public void setUrl(String url) {
        this.url = url;
    }

    //@Override
    public Integer getRssi() {
        return rssi;
    }

    //@Override
    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public double[] getData() {
        return data;
    }

    //@Override
    public void setData(double[] data) {
        this.data = data;
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public String getName() {
        return name;
    }

    //@Override
    public void setName(String name) {
        this.name = name;
    }

    //@Override
    public Double getTemperature() {
        return temperature;
    }

    //@Override
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    //@Override
    public Double getHumidity() {
        return humidity;
    }

    //@Override
    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    //@Override
    public Double getPressure() {
        return pressure;
    }

    //@Override
    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    public Boolean isFavorite() {
        return favorite;
    }

    //@Override
    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    //@Override
    public Double getAccelX() {
        return accelX;
    }

    //@Override
    public void setAccelX(Double accelX) {
        this.accelX = accelX;
    }

    //@Override
    public Double getAccelY() {
        return accelY;
    }

    //@Override
    public void setAccelY(Double accelY) {
        this.accelY = accelY;
    }

    //@Override
    public Double getAccelZ() {
        return accelZ;
    }

    //@Override
    public void setAccelZ(Double accelZ) {
        this.accelZ = accelZ;
    }

    //@Override
    public Double getVoltage() {
        return voltage;
    }

    //@Override
    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public Date getUpdateAt() {
        return updateAt;
    }

    //@Override
    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public String getGatewayUrl() {
        return gatewayUrl;
    }

    //@Override
    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    //@Override
    public Integer getDefaultBackground() {
        return defaultBackground;
    }

    //@Override
    public void setDefaultBackground(Integer defaultBackground) {
        this.defaultBackground = defaultBackground;
    }

    //@Override
    public Integer getDataFormat() {
        return dataFormat;
    }

    //@Override
    public void setDataFormat(Integer dataFormat) {
        this.dataFormat = dataFormat;
    }

    //@Override
    public Double getTxPower() {
        return txPower;
    }

    //@Override
    public void setTxPower(Double txPower) {
        this.txPower = txPower;
    }

    //@Override
    public Integer getMovementCounter() {
        return movementCounter;
    }

    //@Override
    public void setMovementCounter(Integer movementCounter) {
        this.movementCounter = movementCounter;
    }

    //@Override
    @Nullable
    public Integer getMeasurementSequenceNumber() {
        return measurementSequenceNumber;
    }

    //@Override
    public void setMeasurementSequenceNumber(Integer measurementSequenceNumber) {
        this.measurementSequenceNumber = measurementSequenceNumber;
    }

    @org.jetbrains.annotations.Nullable
//@Override
    public String getUserBackground() {
        return userBackground;
    }

    //@Override
    public void setUserBackground(String userBackground) {
        this.userBackground = userBackground;
    }

    //@Override
    @Nullable
    public Boolean getFavorite() {
        return favorite;
    }
}