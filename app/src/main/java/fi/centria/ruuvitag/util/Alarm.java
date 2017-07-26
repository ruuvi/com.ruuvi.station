package fi.centria.ruuvitag.util;

/**
 * Created by tmakinen on 26.7.2017.
 */

public class Alarm {
    int low;
    int high;
    String type;

    public Alarm(int low, int high, String type) {
        this.low = low;
        this.high = high;
        this.type = type;
    }
}
