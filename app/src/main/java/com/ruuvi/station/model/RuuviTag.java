package com.ruuvi.station.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.preference.PreferenceManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public RuuviTag() {
    }

    public RuuviTag(String id, String url, byte[] rawData, int rssi, boolean temporary) {
        this.id = id;
        this.url = url;
        this.rssi = rssi;
        this.rawData = rawData;
        this.rawDataBlob = new Blob(rawData);
        if (!temporary)
            process();
    }

    public RuuviTag(Parcel in) {
        String[] data = new String[6];
        in.readStringArray(data);
        this.id = data[0];
        this.url = data[1];
        this.rssi = Integer.parseInt(data[2]);
        this.temperature = Double.valueOf(data[3]);
        this.humidity = Double.valueOf(data[4]);
        this.pressure = Double.valueOf(data[5]);
    }

    public void updateDataFrom(RuuviTag tag) {
        this.url = tag.url;
        this.rssi = tag.rssi;
        this.rawData = tag.rawData;
        this.updateAt = new Date();
        process();
    }

    private double getFahrenheit() {
        return Utils.celciusToFahrenheit(this.temperature);
    }

    public void process() {
        if (url != null && url.contains("#")) {
            String data = url.split("#")[1];
            rawData = parseByteDataFromB64(data);
            rawDataBlob = new Blob(rawData);
            parseRuuviTagDataFromBytes(rawData, 2);
        } else if (rawData != null) {
            String protocolVersion = String.valueOf(rawData[2]);

            humidity = ((float) (rawData[3] & 0xFF)) / 2f;

            int temperatureSign = (rawData[4] >> 7) & 1;
            int temperatureBase = (rawData[4] & 0x7F);
            float temperatureFraction = ((float) rawData[5]) / 100f;
            temperature = ((float) temperatureBase) + temperatureFraction;
            if (temperatureSign == 1) {
                temperature *= -1;
            }

            int pressureHi = rawData[6] & 0xFF;
            int pressureLo = rawData[7] & 0xFF;
            pressure = pressureHi * 256 + 50000 + pressureLo;
            pressure /= 100.0;

            accelX = (rawData[8] << 8 | rawData[9] & 0xFF) / 1000f;
            accelY = (rawData[10] << 8 | rawData[11] & 0xFF) / 1000f;
            accelZ = (rawData[12] << 8 | rawData[13] & 0xFF) / 1000f;

            int battHi = rawData[14] & 0xFF;
            int battLo = rawData[15] & 0xFF;
            voltage = (battHi * 256 + battLo) / 1000f;

            // make it pretty
            temperature = round(temperature, 2);
            humidity = round(humidity, 2);
            pressure = round(pressure, 2);
            voltage = round(voltage, 4);
            accelX = round(accelX, 4);
            accelY = round(accelY, 4);
            accelZ = round(accelZ, 4);
        }
    }

    private byte[] parseByteDataFromB64(String data) {
        try {
            byte[] bData = base64.decode(data);
            int pData[] = new int[8];
            for (int i = 0; i < bData.length; i++)
                pData[i] = bData[i] & 0xFF;
            return bData;
        } catch (Exception e) {
            return null;
        }
    }

    private void parseRuuviTagDataFromBytes(byte[] bData, int ruuviTagFWVersion) {
        int pData[] = new int[8];
        for (int i = 0; i < bData.length; i++)
            pData[i] = bData[i] & 0xFF;

        if (ruuviTagFWVersion == 1) {
            humidity = ((float) (pData[1] & 0xFF)) / 2f;
            double uTemp = (((pData[3] & 127) << 8) | pData[2]);
            double tempSign = (pData[3] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = ((pData[5] << 8) + pData[4]) + 50000;
            pressure /= 100.00;
            pressure = (pData[7] << 8) + pData[6];
        } else {
            humidity = ((float) (pData[1] & 0xFF)) / 2f;
            double uTemp = (((pData[2] & 127) << 8) | pData[3]);
            double tempSign = (pData[2] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = ((pData[4] << 8) + pData[5]) + 50000;
            pressure /= 100.00;

            //THIS IS UGLY
            temperature = round(temperature, 2);
            humidity = round(humidity, 2);
            pressure = round(pressure, 2);

            this.data = (new double[]{temperature, humidity, pressure});
        }
    }

    public static String getTemperatureUnit(Context context) {
        // TODO: 13/02/2018 move this out to a helper
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