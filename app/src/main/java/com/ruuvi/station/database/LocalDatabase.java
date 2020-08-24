package com.ruuvi.station.database;

import androidx.annotation.NonNull;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.raizlabs.android.dbflow.sql.migration.IndexMigration;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.ruuvi.station.database.tables.Alarm;
import com.ruuvi.station.database.tables.RuuviTagEntity;
import com.ruuvi.station.database.tables.TagSensorReading;
import com.ruuvi.station.database.tables.TagSensorReading_Table;

/**
 * Created by berg on 10/09/17.
 */

@Database(name = LocalDatabase.NAME, version = LocalDatabase.VERSION)
public class LocalDatabase {
    public static final String NAME = "LocalDatabase";
    public static final int VERSION = 13;

    @Migration(version = 13, database = LocalDatabase.class)
    public static class Migration13 extends AlterTableMigration<TagSensorReading> {
        public Migration13 (Class<TagSensorReading> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.REAL, "humidityOffset");
        }
    }

    @Migration(version = 12, database = LocalDatabase.class)
    public static class Migration12 extends AlterTableMigration<RuuviTagEntity> {
        public Migration12(Class<RuuviTagEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.REAL, "humidityOffset");
            addColumn(SQLiteType.INTEGER, "humidityOffsetDate");
        }
    }

    @Migration(version = 11, database = LocalDatabase.class)
    public static class Migration11 extends AlterTableMigration<Alarm> {
        public Migration11(Class<Alarm> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "mutedTill");
        }
    }

    @Migration(version = 11, database = LocalDatabase.class)
    public static class Migration11Data extends BaseMigration {
        @Override
        public void migrate(@NonNull DatabaseWrapper database) {
            database.execSQL("UPDATE TagSensorReading SET pressure = pressure * 100 WHERE pressure < 2000");
            database.execSQL("UPDATE RuuviTag SET pressure = pressure * 100 WHERE pressure < 2000");
        }
    }

    @Migration(version = 10, database = LocalDatabase.class)
    public static class IndexMigration10 extends IndexMigration<TagSensorReading> {
        public IndexMigration10(Class<TagSensorReading> table) {
            super(table);
        }

        @NonNull
        @Override
        public String getName() {
            return "TagId";
        }

        @Override
        public void onPreMigrate() {
            super.onPreMigrate();
            addColumn(TagSensorReading_Table.ruuviTagId);
            addColumn(TagSensorReading_Table.createdAt);
        }
    }

    @Migration(version = 9, database = LocalDatabase.class)
    public static class Migration9 extends AlterTableMigration<RuuviTagEntity> {
        public Migration9(Class<RuuviTagEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "createDate"); }
    }

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
    public static class Migration4 extends AlterTableMigration<RuuviTagEntity> {
        public Migration4(Class<RuuviTagEntity> table) {
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
    public static class Migration3 extends AlterTableMigration<RuuviTagEntity> {
        public Migration3(Class<RuuviTagEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "defaultBackground");
            addColumn(SQLiteType.TEXT, "userBackground");
        }
    }

    @Migration(version = 2, database = LocalDatabase.class)
    public static class Migration2 extends AlterTableMigration<RuuviTagEntity> {
        public Migration2(Class<RuuviTagEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.TEXT, "gatewayUrl");
        }
    }

    @Migration(version = 0, database = LocalDatabase.class)
    public static class IndexMigration0 extends IndexMigration<TagSensorReading> {
        public IndexMigration0(Class<TagSensorReading> table) {
            super(table);
        }

        @NonNull
        @Override
        public String getName() {
            return "TagId";
        }

        @Override
        public void onPreMigrate() {
            super.onPreMigrate();
            addColumn(TagSensorReading_Table.ruuviTagId);
            addColumn(TagSensorReading_Table.createdAt);
        }
    }
}
