package fi.ruuvi.android.model;

/**
 * Created by tmakinen on 26.7.2017.
 */

public class Alarm {
    public int low;
    public int high;
    public String type;

    public Alarm(int low, int high, String type) {
        this.low = low;
        this.high = high;
        this.type = type;
    }
}
