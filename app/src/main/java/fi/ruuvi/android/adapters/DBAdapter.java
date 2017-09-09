package fi.ruuvi.android.adapters;

import android.content.Context;
import android.database.Cursor;

import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import fi.ruuvi.android.R;
import fi.ruuvi.android.database.DBContract;
import fi.ruuvi.android.model.Ruuvitag;
import fi.ruuvi.android.util.Utils;

/**
 * Created by tmakinen on 29.6.2017.
 */

public class DBAdapter extends CursorAdapter {
    private LayoutInflater cursorInflater;
    private Context context;

    public DBAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        cursorInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView txtId = (TextView) view.findViewById(R.id.id);
        TextView txtRssi = (TextView) view.findViewById(R.id.rssi);
        TextView txtTemperature = (TextView) view.findViewById(R.id.temperature);
        TextView txtHumidity = (TextView) view.findViewById(R.id.humidity);
        TextView txtPressure = (TextView) view.findViewById(R.id.pressure);
        TextView txtLast = (TextView) view.findViewById(R.id.lastseen);
        ImageButton edit = (ImageButton) view.findViewById(R.id.edit);
        ImageButton www = (ImageButton) view.findViewById(R.id.openInBrowser);

        edit.setTag(cursor.getInt(0));
        www.setTag(cursor.getInt(0));

        String id = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_ID));
        String rssi = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_RSSI));
        String celsius = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_TEMP));
        String fahrenheit = String.valueOf(Ruuvitag.round(Double.parseDouble(celsius) * 1.8 + 32.0, 2));
        String humidity = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_HUMI));
        String pressure = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_PRES));
        String name = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_NAME));
        String last = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_LAST));
        String url = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_URL));
        if(name != null  && !name.isEmpty())
            txtId.setText(name);
        else
            txtId.setText(id);

        txtRssi.setText(rssi + " dB");
        txtTemperature.setText(celsius + "°C" + " / " + fahrenheit + "°F");
        txtHumidity.setText(humidity + "%");
        txtPressure.setText(pressure + " hPa");




        SimpleDateFormat dateFormat = new SimpleDateFormat(Utils.DB_TIME_FORMAT);
        try
        {
            Date lastSeenDate = dateFormat.parse(last);
           // long date1 = Long.parseLong(last);
           // Calendar calendar = Calendar.getInstance();
           // calendar.setTimeInMillis(date1);

            Date dateNow = new GregorianCalendar().getTime();

            long diffInMS = dateNow.getTime() - lastSeenDate.getTime();

            txtLast.setText(dateFormat.format(lastSeenDate.getTime()) + " / " + url);

            if(diffInMS > 1000*60)
                txtLast.setTextColor(Color.RED);
            else
                txtLast.setTextColor(context.getResources().getColor(R.color.ap_gray));

        }
        catch (Exception e)
        {
            txtLast.setText("MISSING");
            txtLast.setTextColor(Color.RED);
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        this.context = context;
        return cursorInflater.inflate(R.layout.row_item_main, parent, false);
    }
}
