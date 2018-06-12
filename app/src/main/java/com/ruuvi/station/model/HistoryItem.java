package com.ruuvi.station.model;

import java.util.Date;

public class HistoryItem {
    public String date;
    public String minTemp;
    public String maxTemp;
    public String unit;

    public HistoryItem() {
    }

    public HistoryItem(String date, String minTemp, String maxTemp, String unit) {
        this.date = date;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.unit = unit;
    }
}
