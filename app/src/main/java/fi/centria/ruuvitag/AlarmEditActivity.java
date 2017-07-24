package fi.centria.ruuvitag;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Scanner;

import fi.centria.ruuvitag.database.DBContract;
import fi.centria.ruuvitag.database.DBHandler;

import static fi.centria.ruuvitag.R.id.temp_check;

public class AlarmEditActivity extends AppCompatActivity {
    private int index;
    private int[] values;
    private int[] maxValues;
    private CrystalRangeSeekbar rangeSeekbar;
    private Cursor cursor;
    private SQLiteDatabase db;
    private DBHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_edit);

        handler = new DBHandler(this);
        db = handler.getWritableDatabase();

        if(getIntent().getExtras() != null) {
            index = getIntent().getExtras().getInt("index");
        }

        values = new int[2];

        maxValues = new int[]{-40,85,0,100,300,1100};

        // get seekbar from view
        rangeSeekbar = (CrystalRangeSeekbar) findViewById(R.id.rangeSeekbar5);

        // get min and max text view
        final TextView tvMin = (TextView) findViewById(R.id.TextMin1);
        final TextView tvMax = (TextView) findViewById(R.id.TextMax1);

        // set listener
        rangeSeekbar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
            @Override
            public void valueChanged(Number minValue, Number maxValue) {
                tvMin.setText(String.valueOf(minValue));
                tvMax.setText(String.valueOf(maxValue));
            }
        });

        // set final value listener
        rangeSeekbar.setOnRangeSeekbarFinalValueListener(new OnRangeSeekbarFinalValueListener() {
            @Override
            public void finalValue(Number minValue, Number maxValue) {
                values[0] = minValue.intValue();
                values[1] = maxValue.intValue();
            }
        });
    }

    public void loadValues(View view) {
        cursor = db.query(DBContract.RuuvitagDB.TABLE_NAME, null, "_ID= ?", new String[] { "" + index }, null, null, null);
        if(cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_ID));

            String values = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_VALUES));

            if(values == null) {
                values = "-40,85,0,100,300,1100";
            }

            Integer[] valuesArray;
            valuesArray = readSeparated(values);

            for (Integer i : valuesArray) {
                if(i != null)
                    Log.d("tagi", String.valueOf(i));
            }

            if (Objects.equals(view.getTag().toString(), "temperature")) {
                rangeSeekbar.setMinValue(maxValues[0]);
                rangeSeekbar.setMaxValue(maxValues[1]);
                rangeSeekbar.setMinStartValue(12);
                rangeSeekbar.setMaxStartValue(38);
                rangeSeekbar.apply();
            }
            if (Objects.equals(view.getTag().toString(), "humidity")) {}
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            this.save(null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    public void save(View view) {
        CheckBox tempcheck = (CheckBox) findViewById(temp_check);
        if(tempcheck.isEnabled()) {
            
        }


        finish();
    }

    public String commaSeparate(int[] data) {
        if(data != null) {

            String separated = "";

            for (int i : data) {
                separated += String.valueOf(i) + ",";
            }

            return separated.substring(0, separated.length() - 1);
        }
        return null;
    }

    public Integer[] readSeparated(String data) {
        String[] linevector;
        int index = 0;

        linevector = data.split(",");

        Integer[] values = new Integer[linevector.length];

        for(String l : linevector) {
            try {
                values[index] = Integer.parseInt(l);
            } catch (NumberFormatException e) {
                values[index] = null;
            }
            index++;
        }

        return values;
    }
}
