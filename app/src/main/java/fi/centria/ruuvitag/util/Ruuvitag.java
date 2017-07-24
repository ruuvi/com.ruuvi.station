package fi.centria.ruuvitag.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by tmakinen on 15.6.2017.
 */

public class Ruuvitag implements Parcelable {
    private String id;
    private String url;
    private String rssi;
    private double[] data;
    private String name;
    private double temperature;
    private double humidity;
    private double pressure;
    public boolean favorite;
    private byte[] rawData;

    public Ruuvitag(String id, String url, byte[] rawData, String rssi, boolean temporary) {
        this.id = id;
        this.url = url;
        this.rssi = rssi;
        this.rawData = rawData;
        if(!temporary)
            process();
    }

    public Ruuvitag(Beacon beacon, boolean temporary)
    {
        id = beacon.getBluetoothAddress();
        url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
        rssi = String.valueOf(beacon.getRssi());
        if(!temporary)
            process();
    }

    public Ruuvitag(Parcel in) {
        String[] data = new String[6];
        in.readStringArray(data);
        this.id = data[0];
        this.url = data[1];
        this.rssi = data[2];
        this.temperature = Double.valueOf(data[3]);
        this.humidity = Double.valueOf(data[4]);
        this.pressure = Double.valueOf(data[5]);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public String getTemperature() { return String.valueOf(temperature); }

    public String getHumidity() { return String.valueOf(humidity); }

    public String getPressure() { return String.valueOf(pressure); }

    public double[] getData() {
        return data;
    }

    public void setData(double[] data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void process()
    {
        if (url != null && url.contains("#"))
        {
            String data = url.split("#")[1];
            parseRuuvitagDataFromB64(data);
          //  {
            //    parseRuuvitagDataFromB91(data);
            //}
        }
        else if(rawData != null)
        {
            humidity = (rawData[3]) * 0.5;
            double uTemp = (((rawData[4] & 127) << 8) | rawData[5]);
            double tempSign = (rawData[4] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = (rawData[7] & 0xFF) | ((rawData[6] & 0xFF) << 8) + 50000;
            pressure /= 100.00;

            humidity = round(humidity, 2);
            pressure = round(pressure, 2);
            temperature = round(temperature, 2);

            // Acceleration values for each axis
            double x = ((rawData[6] << 8) + rawData[7]);
            x = round(x, 2);
            double y = ((rawData[8] << 8) + rawData[9]);
            y = round(x, 2);
            double z = ((rawData[10] << 8) + rawData[11]);
            z = round(x, 2);
        }
    }

    private boolean parseRuuvitagDataFromB64(String data) {
        try {
            byte[] bData = base64.decode(data);
            // byte[] bData = Base64.decode(data.getBytes(),Base64.NO_PADDING | Base64.NO_WRAP);
            int pData[] = new int[8];
            for (int i = 0; i < bData.length; i++)
                pData[i] = bData[i] & 0xFF;

            //bData[0] must be 2 or 4
            parseByteData(pData,2);

            return true;
        } catch(Exception e) {
            return false;
        }
    }

    private boolean parseRuuvitagDataFromB91(String data) {
        byte[] bData = base91.decode(data.getBytes());
        int pData[] = new int[8];
        for (int i = 0; i < bData.length; i++)
            pData[i] = bData[i] & 0xFF;

        if(pData[0] != 1)
            return false;

        parseByteData(pData, 1);

        return true;
    }

    private void parseByteData(int[] pData, int ruuviTagFWVersion ) {
        if(ruuviTagFWVersion == 1) {
            humidity = pData[1] * 0.5;
            double uTemp = (((pData[3] & 127) << 8) | pData[2]);
            double tempSign = (pData[3] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = ((pData[5] << 8) + pData[4]) + 50000;
            pressure /= 100.00;
            pressure = (pData[7] << 8) + pData[6];
        } else {
            humidity = (pData[1])*0.5;//(int)((pData[1] >> 2) << 11);
            double uTemp = (((pData[2] & 127) << 8) | pData[3]);
            double  tempSign = (pData[2] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = ((pData[4] << 8) + pData[5]) + 50000;
            pressure /= 100.00;

            //THIS IS UGLY
            temperature = round(temperature, 2);
            humidity = round(humidity, 2);
            pressure = round(pressure, 2);

            setData(new double[]{temperature, humidity, pressure});

            /*
            humidity = pData[1] * 0.5;
            double uTemp = (((pData[2] & 127) << 8) | pData[3]);
            double  tempSign = (pData[2] >> 7) & 1;
            temp = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            air_pressure = ((pData[4] << 8) + pData[5]) + 50000;
            air_pressure /= 100.00;
            time_elapsed = (pData[7] << 8) + pData[6];*/
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String [] {this.id, this.url, this.rssi,
                                             String.valueOf(this.temperature),
                                             String.valueOf(this.humidity),
                                             String.valueOf(this.pressure)});
    }

    public static final Parcelable.Creator<Ruuvitag> CREATOR
            = new Parcelable.Creator<Ruuvitag>() {
        public Ruuvitag createFromParcel(Parcel in) {
            return new Ruuvitag(in);
        }

        public Ruuvitag[] newArray(int size) {
            return new Ruuvitag[size];
        }
    };

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}