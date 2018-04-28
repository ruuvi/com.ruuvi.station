package com.ruuvi.station.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.preference.PreferenceManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.android.gms.common.SignInButton;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.data.Blob;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.station.R;
import com.ruuvi.station.database.LocalDatabase;
import com.ruuvi.station.decoder.DecodeFormat2and4;
import com.ruuvi.station.decoder.DecodeFormat3;
import com.ruuvi.station.decoder.DecodeFormat5;
import com.ruuvi.station.decoder.RuuviTagDecoder;
import com.ruuvi.station.util.Utils;
import com.ruuvi.station.util.base64;

import static com.ruuvi.station.util.Utils.round;

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

    public static String getTemperatureUnit(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("pref_temperature_unit", "C");
    }

    public String getTemperatureString(Context context) {
        String temperatureUnit = RuuviTag.getTemperatureUnit(context);
        if (temperatureUnit.equals("C")) {
            return String.format(context.getString(R.string.temperature_reading), this.temperature) + temperatureUnit;
        }
        return String.format(context.getString(R.string.temperature_reading), this.getFahrenheit()) + temperatureUnit;
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