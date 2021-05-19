package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.annotation.Database
import com.raizlabs.android.dbflow.annotation.Migration
import com.raizlabs.android.dbflow.sql.SQLiteType
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration
import com.raizlabs.android.dbflow.sql.migration.BaseMigration
import com.raizlabs.android.dbflow.sql.migration.IndexMigration
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper
import com.ruuvi.station.database.tables.*

@Database(name = LocalDatabase.NAME, version = LocalDatabase.VERSION)
class LocalDatabase {
    companion object {
        const val NAME = "LocalDatabase"
        const val VERSION = 23
    }

    @Migration(version = 23, database = LocalDatabase::class)
    class Migration23 : BaseMigration() {
        override fun migrate(database: DatabaseWrapper) {
            database.execSQL(
                "UPDATE SensorSettings " +
                "SET name = 'Ruuvi ' || substr(id,13,2) || substr(id,16,2) " +
                "WHERE (name is NULL or name = '') and LENGTH(id) = 17"
            )
        }
    }

    @Migration(version = 22, database = LocalDatabase::class)
    class Migration22 : BaseMigration() {
        override fun migrate(database: DatabaseWrapper) {
            database.execSQL("INSERT INTO SensorSettings (id) SELECT RT.id FROM RuuviTag RT left join SensorSettings SS on RT.id =SS.id WHERE favorite=1 and SS.id is null;")
            database.execSQL("UPDATE SensorSettings SET " +
                "name = (SELECT name FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id), " +
                "networkBackground = (SELECT networkBackground FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id), " +
                "userBackground = (SELECT userBackground FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id), " +
                "defaultBackground = (SELECT defaultBackground FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id), " +
                "createDate = (SELECT createDate FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id), " +
                "lastSync = (SELECT lastSync FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id), " +
                "networkLastSync = (SELECT networkLastSync FROM RuuviTag WHERE RuuviTag.id = SensorSettings.id);")
        }
    }

    @Migration(version = 21, database = LocalDatabase::class)
    class Migration21(table: Class<SensorSettings?>?) : AlterTableMigration<SensorSettings?>(table) {
        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(SQLiteType.TEXT, "name")
            addColumn(SQLiteType.INTEGER, "createDate")
            addColumn(SQLiteType.INTEGER, "lastSync")
            addColumn(SQLiteType.INTEGER, "networkLastSync")
            addColumn(SQLiteType.TEXT, "owner")
            addColumn(SQLiteType.TEXT, "networkBackground")
            addColumn(SQLiteType.TEXT, "userBackground")
            addColumn(SQLiteType.TEXT, "defaultBackground")
        }
    }

    @Migration(version = 20, database = LocalDatabase::class)
    class Migration20_1 : BaseMigration() {
        override fun migrate(database: DatabaseWrapper) {
            database.execSQL("INSERT INTO SensorSettings (id, humidityOffset, humidityOffsetDate) SELECT id, humidityOffset, humidityOffsetDate FROM RuuviTag WHERE favorite=1")
        }
    }

    @Migration(version = 20, database = LocalDatabase::class)
    class Migration20_2(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(SQLiteType.REAL, "temperatureOffset")
            addColumn(SQLiteType.REAL, "pressureOffset")
        }
    }

    @Migration(version = 20, database = LocalDatabase::class)
    class Migration20_3(table: Class<TagSensorReading?>?) : AlterTableMigration<TagSensorReading?>(table) {
        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(SQLiteType.REAL, "temperatureOffset")
            addColumn(SQLiteType.REAL, "pressureOffset")
        }
    }

    @Migration(version = 19, database = LocalDatabase::class)
    class Migration19(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(SQLiteType.TEXT, "networkBackground")
        }
    }

    @Migration(version = 17, database = LocalDatabase::class)
    class Migration17(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(SQLiteType.INTEGER, "networkLastSync")
        }
    }

    @Migration(version = 16, database = LocalDatabase::class)
    class Migration16(table: Class<Alarm?>?) : AlterTableMigration<Alarm?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.TEXT, "customDescription")
        }
    }

    @Migration(version = 15, database = LocalDatabase::class)
    class Migration15(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(SQLiteType.INTEGER, "connectable")
            addColumn(SQLiteType.INTEGER, "lastSync")
        }
    }

    @Migration(version = 14, database = LocalDatabase::class)
    class Migration14Data : BaseMigration() {
        override fun migrate(database: DatabaseWrapper) {
            database.execSQL("UPDATE Alarm SET low = low * 100, high = high * 100 WHERE type = 2 and (low < 2000 or high < 2000)")
        }
    }

    @Migration(version = 13, database = LocalDatabase::class)
    class Migration13(table: Class<TagSensorReading?>?) : AlterTableMigration<TagSensorReading?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.REAL, "humidityOffset")
        }
    }

    @Migration(version = 12, database = LocalDatabase::class)
    class Migration12(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.REAL, "humidityOffset")
            addColumn(SQLiteType.INTEGER, "humidityOffsetDate")
        }
    }

    @Migration(version = 11, database = LocalDatabase::class)
    class Migration11(table: Class<Alarm?>?) : AlterTableMigration<Alarm?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "mutedTill")
        }
    }

    @Migration(version = 11, database = LocalDatabase::class)
    class Migration11Data : BaseMigration() {
        override fun migrate(database: DatabaseWrapper) {
            database.execSQL("UPDATE TagSensorReading SET pressure = pressure * 100 WHERE pressure < 2000")
            database.execSQL("UPDATE RuuviTag SET pressure = pressure * 100 WHERE pressure < 2000")
        }
    }

    @Migration(version = 10, database = LocalDatabase::class)
    class IndexMigration10(table: Class<TagSensorReading?>?) : IndexMigration<TagSensorReading?>(table!!) {
        override fun getName(): String {
            return "TagId"
        }

        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(TagSensorReading_Table.ruuviTagId)
            addColumn(TagSensorReading_Table.createdAt)
        }
    }

    @Migration(version = 9, database = LocalDatabase::class)
    class Migration9(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "createDate")
        }
    }

    @Migration(version = 6, database = LocalDatabase::class)
    class Migration6(table: Class<Alarm?>?) : AlterTableMigration<Alarm?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "enabled")
        }
    }

    @Migration(version = 5, database = LocalDatabase::class)
    class Migration5(table: Class<TagSensorReading?>?) : AlterTableMigration<TagSensorReading?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "dataFormat")
            addColumn(SQLiteType.REAL, "txPower")
            addColumn(SQLiteType.INTEGER, "movementCounter")
            addColumn(SQLiteType.INTEGER, "measurementSequenceNumber")
        }
    }

    @Migration(version = 4, database = LocalDatabase::class)
    class Migration4(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "dataFormat")
            addColumn(SQLiteType.REAL, "txPower")
            addColumn(SQLiteType.INTEGER, "movementCounter")
            addColumn(SQLiteType.INTEGER, "measurementSequenceNumber")
        }
    }

    @Migration(version = 3, database = LocalDatabase::class)
    class Migration3(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "defaultBackground")
            addColumn(SQLiteType.TEXT, "userBackground")
        }
    }

    @Migration(version = 2, database = LocalDatabase::class)
    class Migration2(table: Class<RuuviTagEntity?>?) : AlterTableMigration<RuuviTagEntity?>(table) {
        override fun onPreMigrate() {
            addColumn(SQLiteType.TEXT, "gatewayUrl")
        }
    }

    @Migration(version = 0, database = LocalDatabase::class)
    class IndexMigration0(table: Class<TagSensorReading?>?) : IndexMigration<TagSensorReading?>(table!!) {
        override fun getName(): String {
            return "TagId"
        }

        override fun onPreMigrate() {
            super.onPreMigrate()
            addColumn(TagSensorReading_Table.ruuviTagId)
            addColumn(TagSensorReading_Table.createdAt)
        }
    }
}