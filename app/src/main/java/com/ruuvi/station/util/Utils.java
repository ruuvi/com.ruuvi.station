package com.ruuvi.station.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.ruuvi.station.R;
import com.ruuvi.station.database.tables.RuuviTagEntity;
import com.ruuvi.station.tag.domain.RuuviTag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import androidx.core.content.ContextCompat;
import timber.log.Timber;


public class Utils {


    public static final String emojiRegex =
            "(?:[\\u2700-\\u27bf]|(?:[\\ud83c\\udde6-\\ud83c\\uddff]){2}|[\\ud800\\udc00-\\uDBFF\\uDFFF]|" +
            "[\\u2600-\\u26FF])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe23\\u20d0-\\u20f0]|" +
            "[\\ud83c\\udffb-\\ud83c\\udfff])?(?:\\u200d(?:[^\\ud800-\\udfff]|(?:[\\ud83c\\udde6-\\ud83c\\uddff]){2}|" +
            "[\\ud800\\udc00-\\uDBFF\\uDFFF]|[\\u2600-\\u26FF])[\\ufe0e\\ufe0f]?(?:" +
            "[\\u0300-\\u036f\\ufe20-\\ufe23\\u20d0-\\u20f0]|[\\ud83c\\udffb-\\ud83c\\udfff])?)*|" +
            "[\\u0023-\\u0039]\\ufe0f?\\u20e3|\\u3299|\\u3297|\\u303d|\\u3030|\\u24c2|[\\ud83c\\udd70-\\ud83c\\udd71]|" +
            "[\\ud83c\\udd7e-\\ud83c\\udd7f]|\\ud83c\\udd8e|[\\ud83c\\udd91-\\ud83c\\udd9a]|" +
            "[\\ud83c\\udde6-\\ud83c\\uddff]|[\\ud83c\\ude01-\\ud83c\\ude02]|\\ud83c\\ude1a|\\ud83c\\ude2f|" +
            "[\\ud83c\\ude32-\\ud83c\\ude3a]|[\\ud83c\\ude50-\\ud83c\\ude51]|\\u203c|\\u2049|" +
            "[\\u25aa-\\u25ab]|\\u25b6|\\u25c0|[\\u25fb-\\u25fe]|\\u00a9|\\u00ae|\\u2122|\\u2139|\\ud83c\\udc04|" +
            "[\\u2600-\\u26FF]|\\u2b05|\\u2b06|\\u2b07|\\u2b1b|\\u2b1c|\\u2b50|\\u2b55|\\u231a|\\u231b|\\u2328|\\u23cf|" +
            "[\\u23e9-\\u23f3]|[\\u23f8-\\u23fa]|\\ud83c\\udccf|\\u2934|\\u2935|[\\u2190-\\u21ff]";

    public static Bitmap createBall(int radius, int ballColor, int letterColor, String deviceId, float letterSize) {
        String letter = deviceId.matches(emojiRegex) ? deviceId : ("" + deviceId.charAt(0)).toUpperCase();
        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint2 = new Paint();
        paint2.setColor(ballColor);
        canvas.drawCircle(radius, radius, (float) radius, paint2);
        Paint paint = new Paint();
        paint.setColor(letterColor);
        paint.setTextSize(letterSize);
        paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        Rect textBounds = new Rect();
        paint.getTextBounds(letter, 0, letter.length(), textBounds);
        canvas.drawText(letter, radius - textBounds.exactCenterX(), radius - textBounds.exactCenterY(), paint);
        return bitmap;
    }

    public static String strDescribingTimeSince(Date date) {
        String output = "";
        Date dateNow = new Date();
        long diffInMS = dateNow.getTime() - date.getTime();
        // show date if the tag has not been seen for 24h
        if (diffInMS > 24 * 60 * 60 * 1000) {
            output += date.toString();
        } else {
            int seconds = (int) (diffInMS / 1000) % 60;
            int minutes = (int) ((diffInMS / (1000 * 60)) % 60);
            int hours = (int) ((diffInMS / (1000 * 60 * 60)) % 24);
            if (hours > 0) output += hours + " h ";
            if (minutes > 0) output += minutes + " min ";
            output += seconds + " s ago";
        }
        return output;
    }

    public static Bitmap getBackground(Context context, RuuviTagEntity tag) {
        try {
            Uri uri = Uri.parse(tag.getUserBackground());
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (Exception e) {
            Timber.e("Could not set user background");
        }

        return BitmapFactory.decodeResource(context.getResources(), getDefaultBackground(tag.getDefaultBackground()));
    }

    public static Bitmap getBackground(Context context, RuuviTag tag) {
        try {
            Uri uri = Uri.parse(tag.getUserBackground());
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (Exception e) {
            Timber.e("Could not set user background");
        }

        return BitmapFactory.decodeResource(context.getResources(), getDefaultBackground(tag.getDefaultBackground()));
    }


    public static Drawable getDefaultBackground(int number, Context context) {
        return ContextCompat.getDrawable(context, getDefaultBackground(number));
    }

    private static int getDefaultBackground(int number) {
        switch (number) {
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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
