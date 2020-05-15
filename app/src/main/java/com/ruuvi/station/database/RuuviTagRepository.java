package com.ruuvi.station.database;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.station.R;
import com.ruuvi.station.database.tables.Alarm;
import com.ruuvi.station.database.tables.Alarm_Table;
import com.ruuvi.station.database.tables.RuuviTagEntity_Table;
import com.ruuvi.station.database.tables.TagSensorReading_Table;
import com.ruuvi.station.model.HumidityUnit;
import com.ruuvi.station.database.tables.RuuviTagEntity;
import com.ruuvi.station.database.tables.TagSensorReading;
import com.ruuvi.station.util.Humidity;
import com.ruuvi.station.app.preferences.Preferences;
import com.ruuvi.station.util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class RuuviTagRepository {

    public static String getDispayName(RuuviTagEntity tag) {
        return (tag.getName() != null && !tag.getName().isEmpty()) ? tag.getName() : tag.getId();
    }

    public static List<RuuviTagEntity> getAll(boolean favorite) {
        return SQLite.select()
                .from(RuuviTagEntity.class)
                .where(RuuviTagEntity_Table.favorite.eq(favorite))
                .orderBy(RuuviTagEntity_Table.createDate, true)
                .queryList();
    }

    public static RuuviTagEntity get(String id) {
        return SQLite.select()
                .from(RuuviTagEntity.class)
                .where(RuuviTagEntity_Table.id.eq(id))
                .querySingle();
    }

    public static void deleteTagAndRelatives(RuuviTagEntity tag) {
        SQLite.delete()
                .from(Alarm.class)
                .where(Alarm_Table.ruuviTagId.eq(tag.getId()))
                .execute();
        SQLite.delete()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(tag.getId()))
                .execute();

        tag.delete();
    }

    private static double getFahrenheit(RuuviTagEntity tag) {
        return Utils.celciusToFahrenheit(tag.getTemperature());
    }

    private static double getKelvin(RuuviTagEntity tag) {
        return Utils.celsiusToKelvin(tag.getTemperature());
    }

    public static HumidityUnit getHumidityUnit(Context context) {
        return new Preferences(context).getHumidityUnit();
    }

    public static String getHumidityString(Context context, RuuviTagEntity tag) {
        HumidityUnit humidityUnit = getHumidityUnit(context);
        Humidity calculation = new Humidity(tag.getTemperature(), tag.getHumidity() / 100.0);
        switch (humidityUnit) {
            case PERCENT:
                return String.format(context.getString(R.string.humidity_reading), tag.getHumidity());
            case GM3:
                return String.format(context.getString(R.string.humidity_absolute_reading), calculation.getAh());
            case DEW:
                String temperatureUnit = getTemperatureUnit(context);
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

    public static String getTemperatureString(Context context, RuuviTagEntity tag) {
        String temperatureUnit = getTemperatureUnit(context);
        String formatTemplate = context.getString(R.string.temperature_reading);
        switch (temperatureUnit) {
            case "C":
                return String.format(formatTemplate, tag.getTemperature()) + "°" + temperatureUnit;
            case "K":
                return String.format(formatTemplate, getKelvin(tag)) + temperatureUnit;
            case "F":
                return String.format(formatTemplate, getFahrenheit(tag)) + "°" + temperatureUnit;
            default:
                return "Error";
        }
    }

    public static String getTemperatureUnit(Context context) {
        return new Preferences(context).getTemperatureUnit();
    }

    public static void update(RuuviTagEntity tag) {
        tag.update();
    }

    public static void save(@NotNull RuuviTagEntity tag) {
        tag.save();
    }
}