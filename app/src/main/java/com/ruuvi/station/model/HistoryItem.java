package com.ruuvi.station.model;

import java.util.Date;

public class HistoryItem {
    public Date date;
    public double minTemp;
    public double maxTemp;
    public String unit;

    public HistoryItem() {
    }

    public HistoryItem(Date date, double minTemp, double maxTemp, String unit) {
        this.date = date;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.unit = unit;
    }
}
