package com.ruuvi.station.database;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
import com.ruuvi.station.model.Alarm;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.TagSensorReading;

/**
 * Created by berg on 10/09/17.
 */

@Database(name = LocalDatabase.NAME, version = LocalDatabase.VERSION)
public class LocalDatabase {
    public static final String NAME = "LocalDatabase";
    public static final int VERSION = 7;

    @Migration(version = 6, database = LocalDatabase.class)
    public static class Migration6 extends AlterTableMigration<Alarm> {
        public Migration6(Class<Alarm> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "enabled");
        }
    }

    @Migration(version = 5, database = LocalDatabase.class)
    public static class Migration5 extends AlterTableMigration<TagSensorReading> {
        public Migration5(Class<TagSensorReading> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "dataFormat");
            addColumn(SQLiteType.REAL, "txPower");
            addColumn(SQLiteType.INTEGER, "movementCounter");
            addColumn(SQLiteType.INTEGER, "measurementSequenceNumber");
        }
    }

    @Migration(version = 4, database = LocalDatabase.class)
    public static class Migration4 extends AlterTableMigration<RuuviTag> {
        public Migration4(Class<RuuviTag> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "dataFormat");
            addColumn(SQLiteType.REAL, "txPower");
            addColumn(SQLiteType.INTEGER, "movementCounter");
            addColumn(SQLiteType.INTEGER, "measurementSequenceNumber");
        }
    }

    @Migration(version = 3, database = LocalDatabase.class)
    public static class Migration3 extends AlterTableMigration<RuuviTag> {
        public Migration3(Class<RuuviTag> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "defaultBackground");
            addColumn(SQLiteType.TEXT, "userBackground");
        }
    }

    @Migration(version = 2, database = LocalDatabase.class)
    public static class Migration2 extends AlterTableMigration<RuuviTag> {
        public Migration2(Class<RuuviTag> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.TEXT, "gatewayUrl");
        }
    }
}
