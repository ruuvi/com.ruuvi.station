package com.ruuvi.station.database;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.station.R;
import com.ruuvi.station.bluetooth.interfaces.IRuuviTag;
import com.ruuvi.station.model.Alarm;
import com.ruuvi.station.model.Alarm_Table;
import com.ruuvi.station.model.RuuviTagEntity;
import com.ruuvi.station.model.RuuviTagEntity_Table;
import com.ruuvi.station.model.TagSensorReading;
import com.ruuvi.station.model.TagSensorReading_Table;
import com.ruuvi.station.util.Preferences;
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

    public static String getTemperatureString(Context context, RuuviTagEntity tag) {
        String temperatureUnit = getTemperatureUnit(context);
        if (temperatureUnit.equals("C")) {
            return String.format(context.getString(R.string.temperature_reading), tag.getTemperature()) + temperatureUnit;
        }
        return String.format(context.getString(R.string.temperature_reading), getFahrenheit(tag)) + temperatureUnit;
    }

    public static double getFahrenheit(RuuviTagEntity tag) {
        return Utils.celciusToFahrenheit(tag.getTemperature());
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