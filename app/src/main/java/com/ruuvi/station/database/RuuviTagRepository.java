package com.ruuvi.station.database;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.station.R;
import com.ruuvi.station.model.Alarm;
import com.ruuvi.station.model.Alarm_Table;
import com.ruuvi.station.bluetooth.domain.IRuuviTag;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.RuuviTag_Table;
import com.ruuvi.station.model.TagSensorReading;
import com.ruuvi.station.model.TagSensorReading_Table;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.Utils;

import java.util.List;


public class RuuviTagRepository {

    public static String getDispayName(IRuuviTag tag) {
        return (tag.getName() != null && !tag.getName().isEmpty()) ? tag.getName() : tag.getId();
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

    public static void deleteTagAndRelatives(IRuuviTag tag) {
        SQLite.delete()
                .from(Alarm.class)
                .where(Alarm_Table.ruuviTagId.eq(tag.getId()))
                .execute();
        SQLite.delete()
                .from(TagSensorReading.class)
                .where(TagSensorReading_Table.ruuviTagId.eq(tag.getId()))
                .execute();

        // FIXME: remove cast
        ((com.ruuvi.station.model.RuuviTag) tag).delete();
    }

    public static String getTemperatureString(Context context, IRuuviTag tag) {
        String temperatureUnit = getTemperatureUnit(context);
        if (temperatureUnit.equals("C")) {
            return String.format(context.getString(R.string.temperature_reading), tag.getTemperature()) + temperatureUnit;
        }
        return String.format(context.getString(R.string.temperature_reading), getFahrenheit(tag)) + temperatureUnit;
    }

    public static double getFahrenheit(IRuuviTag tag) {
        return Utils.celciusToFahrenheit(tag.getTemperature());
    }

    public static String getTemperatureUnit(Context context) {
        return new Preferences(context).getTemperatureUnit();
    }

}