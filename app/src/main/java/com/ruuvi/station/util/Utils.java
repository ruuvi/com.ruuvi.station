package com.ruuvi.station.util;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.util.Log;

import com.ruuvi.station.R;
import com.ruuvi.station.model.RuuviTag;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by admin on 09/09/2017.
 */

public class Utils {
    private static final String TAG = "Utils";
    public static final java.lang.String DB_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    public static boolean tryParse(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Bitmap createBall(int radius, int ballColor, int letterColor, String letter) {
        letter = letter.toUpperCase();
        Bitmap bitmap = Bitmap.createBitmap(radius*2, radius*2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint2 = new Paint();
        paint2.setColor(ballColor);
        canvas.drawCircle(radius, radius, (float) radius, paint2);
        Paint paint = new Paint();
        paint.setColor(letterColor);
        paint.setTextSize(100);
        paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        Rect textBounds = new Rect();
        paint.getTextBounds(letter, 0, letter.length(), textBounds);
        canvas.drawText(letter, radius - textBounds.exactCenterX(), radius - textBounds.exactCenterY(), paint);
        return bitmap;
    }

    public static void sortTagsByRssi(List<RuuviTag> tags) {
        Collections.sort(tags, new Comparator<RuuviTag>() {
            @Override public int compare(RuuviTag o1, RuuviTag o2) {
                return o2.rssi - o1.rssi;
            }
        });
    }

    public static byte[] parseByteDataFromB64(String data) {
        try {
            byte[] bData = base64.decode(data);
            int pData[] = new int[8];
            for (int i = 0; i < bData.length; i++)
                pData[i] = bData[i] & 0xFF;
            return bData;
        } catch (Exception e) {
            return null;
        }
    }

    public static String strDescribingTimeSince(Date date) {
        String output = "";
        Date dateNow = new Date();
        long diffInMS = dateNow.getTime() - date.getTime();
        // show date if the tag has not been seen for 24h
        if (diffInMS > 24 * 60 * 60 * 1000) {
            output += date.toString();
        } else {
            int seconds = (int) (diffInMS / 1000) % 60 ;
            int minutes = (int) ((diffInMS / (1000*60)) % 60);
            int hours   = (int) ((diffInMS / (1000*60*60)) % 24);
            if (hours > 0) output += hours + " h ";
            if (minutes > 0) output += minutes + " min ";
            output += seconds + " s ago";
        }
        return output;
    }

    public static Bitmap getBackground(Context context, RuuviTag tag) {
        try {
            Uri uri = Uri.parse(tag.userBackground);
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (Exception e) {
            Log.e(TAG, "Could not set user background");
        }

        return BitmapFactory.decodeResource(context.getResources(), getDefaultBackground(tag.defaultBackground));
    }


    public static Drawable getDefaultBackground(int number, Context context) {
        return context.getResources().getDrawable(getDefaultBackground(number));
    }

    private static int getDefaultBackground(int number) {
        switch (number) {
            case 0:
                return R.drawable.bg1;
            case 1:
                return R.drawable.bg2;
            case 2:
                return R.drawable.bg3;
            case 3:
                return R.drawable.bg4;
            case 4:
                return R.drawable.bg5;
            case 5:
                return R.drawable.bg6;
            case 6:
                return R.drawable.bg7;
            case 7:
                return R.drawable.bg8;
            case 8:
                return R.drawable.bg9;
            default:
                return R.drawable.bg1;
        }
    }

    public static double celciusToFahrenheit(double celcius) {
        return round(celcius * 1.8 + 32.0, 2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static boolean removeStateFile(Context context) {
        String path = context.getFilesDir().getPath() + "/android-beacon-library-scan-state";
        return new File(path).delete();
    }

    public static List<ScanFilter> getScanFilters() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter ruuviFilter = new ScanFilter.Builder()
                .setManufacturerData(0x0499, new byte[] {})
                .build();
        ScanFilter eddystoneFilter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb"))
                .build();
        filters.add(ruuviFilter);
        filters.add(eddystoneFilter);
        return filters;
    }

    public static void setAltBeaconParsers(BeaconManager beaconManager) {
        beaconManager.getBeaconParsers().clear();

        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));

        BeaconParser v3Parser = new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT);
        v3Parser.setHardwareAssistManufacturerCodes(new int[]{1177});
        beaconManager.getBeaconParsers().add(v3Parser);

        BeaconParser v5Parser = new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT);
        v5Parser.setHardwareAssistManufacturerCodes(new int[]{1177});
        beaconManager.getBeaconParsers().add(v5Parser);
    }
}
