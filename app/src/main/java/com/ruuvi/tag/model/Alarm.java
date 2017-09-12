package com.ruuvi.tag.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.ruuvi.tag.database.LocalDatabase;

/**
 * Created by tmakinen on 26.7.2017.
 */

@Table(database = LocalDatabase.class)
public class Alarm extends BaseModel {
    @Column
    @PrimaryKey(autoincrement = true)
    public int id;
    @Column
    public String ruuviTagId;
    @Column
    public int low;
    @Column
    public int high;
    @Column
    public String type;

    public Alarm() {
    }

    public Alarm(int low, int high, String type) {
        this.low = low;
        this.high = high;
        this.type = type;
    }
}
