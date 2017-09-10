package com.ruuvi.tag.database;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by berg on 10/09/17.
 */

@Database(name = LocalDatabase.NAME, version = LocalDatabase.VERSION)
public class LocalDatabase {
    public static final String NAME = "LocalDatabase";
    public static final int VERSION = 1;
}
