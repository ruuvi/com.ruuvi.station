package com.ruuvi.tag.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.ruuvi.tag.util.base64;

/**
 * Created by tmakinen on 15.6.2017.
 */

public class RuuviTag implements Parcelable {
    public String id;
    public String url;
    public String rssi;
    public double[] data;
    public String name;
    public double temperature;
    public double humidity;
    public double pressure;
    public boolean favorite;
    public byte[] rawData;
    public double accelX;
    public double accelY;
    public double accelZ;
    public double voltage;

    public RuuviTag(String id, String url, byte[] rawData, String rssi, boolean temporary) {
        this.id = id;
        this.url = url;
        this.rssi = rssi;
        this.rawData = rawData;
        if(!temporary)
            process();
    }

    public RuuviTag(Beacon beacon, boolean temporary)
    {
        id = beacon.getBluetoothAddress();
        url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
        rssi = String.valueOf(beacon.getRssi());
        if(!temporary)
            process();
    }

    public RuuviTag(Parcel in) {
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

    public void process()
    {
        if (url != null && url.contains("#"))
        {
            String data = url.split("#")[1];
            rawData = parseByteDataFromB64(data);
            parseRuuviTagDataFromBytes(rawData,2);
        }
        else if(rawData != null)
        {
            humidity = ((float) (rawData[3] & 0xFF)) / 2f;

            double uTemp = (((rawData[4] & 127) << 8) | rawData[5]);
            double tempSign = (rawData[4] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = (rawData[7] & 0xFF) | ((rawData[6] & 0xFF) << 8) + 50000;
            pressure /= 100.00;

            humidity = round(humidity, 2);
            pressure = round(pressure, 2);
            temperature = round(temperature, 2);

            // Acceleration values for each axis
            double x = ((rawData[8] << 8) + rawData[9]);
            accelX = round(x, 2);
            double y = ((rawData[10] << 8) + rawData[11]);
            accelY = round(y, 2);
            double z = ((rawData[12] << 8) + rawData[13]);
            accelZ = round(z, 2);

            voltage = ((rawData[15] & 0xFF) | ((rawData[14] & 0xFF) << 8)) / 100.0;
        }
    }

    private byte[] parseByteDataFromB64(String data)
    {
        try
        {
            byte[] bData = base64.decode(data);
            int pData[] = new int[8];
            for (int i = 0; i < bData.length; i++)
                pData[i] = bData[i] & 0xFF;
            return bData;
        } catch(Exception e) {
            return null;
        }
    }

    private void parseRuuviTagDataFromBytes(byte[] bData, int ruuviTagFWVersion )
    {
        int pData[] = new int[8];
        for (int i = 0; i < bData.length; i++)
            pData[i] = bData[i] & 0xFF;

        if(ruuviTagFWVersion == 1) {
            humidity = ((float) (pData[1] & 0xFF)) / 2f;
            double uTemp = (((pData[3] & 127) << 8) | pData[2]);
            double tempSign = (pData[3] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = ((pData[5] << 8) + pData[4]) + 50000;
            pressure /= 100.00;
            pressure = (pData[7] << 8) + pData[6];
        }
        else
        {
            humidity = ((float) (pData[1] & 0xFF)) / 2f;
            double uTemp = (((pData[2] & 127) << 8) | pData[3]);
            double  tempSign = (pData[2] >> 7) & 1;
            temperature = tempSign == 0.00 ? uTemp / 256.0 : -1.00 * uTemp / 256.0;
            pressure = ((pData[4] << 8) + pData[5]) + 50000;
            pressure /= 100.00;

            //THIS IS UGLY
            temperature = round(temperature, 2);
            humidity = round(humidity, 2);
            pressure = round(pressure, 2);

            this.data = (new double[]{temperature, humidity, pressure});
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String [] {this.id, this.url, this.rssi,
                                             String.valueOf(this.temperature),
                                             String.valueOf(this.humidity),
                                             String.valueOf(this.pressure)});
    }

    public static final Parcelable.Creator<RuuviTag> CREATOR
            = new Parcelable.Creator<RuuviTag>() {
        public RuuviTag createFromParcel(Parcel in) {
            return new RuuviTag(in);
        }

        public RuuviTag[] newArray(int size) {
            return new RuuviTag[size];
        }
    };

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}