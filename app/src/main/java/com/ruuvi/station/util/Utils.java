package com.ruuvi.station.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.ruuvi.station.R;
import com.ruuvi.station.model.RuuviTag;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by admin on 09/09/2017.
 */

public class Utils {
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
        paint.setTextSize(120);
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

    public static Drawable getDefaultBackground(int number, Context context) {
        switch (number) {
            case 0:
                return context.getResources().getDrawable(R.drawable.bg1);
            case 1:
                return context.getResources().getDrawable(R.drawable.bg2);
            case 2:
                return context.getResources().getDrawable(R.drawable.bg3);
            default:
                return context.getResources().getDrawable(R.drawable.bg1);
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
}
