package fi.centria.ruuvitag.database;

import android.provider.BaseColumns;

/**
 * Created by tmakinen on 29.6.2017.
 */

public final class DBContract {
    private DBContract() { }

    public static class RuuvitagDB implements BaseColumns {
        public static final String TABLE_NAME = "ruuvitag";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_URL = "url";
        public static final String COLUMN_RSSI = "rssi";
        public static final String COLUMN_TEMP = "temperature";
        public static final String COLUMN_HUMI = "humidity";
        public static final String COLUMN_PRES = "pressure";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LAST = "lastseen";
        public static final String COLUMN_COLOR = "color";

        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + "(" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ID + " TEXT, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_RSSI + " TEXT, " +
                COLUMN_TEMP + " TEXT, " +
                COLUMN_HUMI + " TEXT, " +
                COLUMN_PRES + " TEXT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_COLOR + " TEXT, " +
                COLUMN_LAST + " TEXT )";
    }
}
