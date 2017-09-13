package com.ruuvi.tag.feature.edit;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.ruuvi.tag.R;
import com.ruuvi.tag.model.Alarm;

import java.util.AbstractMap;
import java.util.List;

public class AlarmEditActivity extends AppCompatActivity {
    private String tagId;
    private int[] maxValues;
    private CrystalRangeSeekbar rangeSeekbar;
    private TextView temp;
    private int viewTag;
    private Integer[] valuesArray;
    private List<Alarm> alarms;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_edit);
        maxValues = new int[]{-40,85,0,100,300,1100,-100,0};
        valuesArray = new Integer[8];

        if(getIntent().getExtras() != null) {
            tagId = getIntent().getExtras().getString("tagId");
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.tag_not_found), Toast.LENGTH_SHORT).show();
            finish();
        }
        alarms = Alarm.getForTag(tagId);

        for (Alarm alarm: alarms) {
            if (alarm.type == Alarm.TEMPERATURE) ((CheckBox)findViewById(R.id.check_temp)).setChecked(true);
            if (alarm.type == Alarm.HUMIDITY) ((CheckBox)findViewById(R.id.check_humi)).setChecked(true);
            if (alarm.type == Alarm.PERSSURE) ((CheckBox)findViewById(R.id.check_pres)).setChecked(true);
            if (alarm.type == Alarm.RSSI) ((CheckBox)findViewById(R.id.check_rssi)).setChecked(true);
        }

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadValues(findViewById(R.id.text_temp));
    }

    public void loadValues(View view) {
        if(temp != null) {
            temp.setPaintFlags(temp.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
        }

        temp = (TextView) findViewById(view.getId());
        temp.setPaintFlags(temp.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        temp.setText(temp.getText());
        viewTag = Integer.parseInt(view.getTag().toString());
        Alarm theAlarm = null;
        for (Alarm alarm: alarms) {
            if (alarm.type == viewTag) theAlarm = alarm;
        }
        if (theAlarm == null) {
            int arrOffset = 0;
            if (viewTag == Alarm.HUMIDITY) arrOffset = 2;
            else if (viewTag == Alarm.PERSSURE) arrOffset = 4;
            else if (viewTag == Alarm.RSSI) arrOffset = 6;
            theAlarm = new Alarm(maxValues[arrOffset], maxValues[arrOffset + 1], viewTag, tagId);
            alarms.add(theAlarm);
        }

        switch(viewTag) {
            case Alarm.TEMPERATURE: {
                rangeSeekbar.setMinValue(maxValues[0]);
                rangeSeekbar.setMaxValue(maxValues[1]);
                break;
                }
            case Alarm.HUMIDITY: {
                rangeSeekbar.setMinValue(maxValues[2]);
                rangeSeekbar.setMaxValue(maxValues[3]);
                break;
            }
            case Alarm.PERSSURE: {
                rangeSeekbar.setMinValue(maxValues[4]);
                rangeSeekbar.setMaxValue(maxValues[5]);
                break;
            }
            case Alarm.RSSI: {
                rangeSeekbar.setMinValue(maxValues[6]);
                rangeSeekbar.setMaxValue(maxValues[7]);
                break;
            }
        }
        rangeSeekbar.setMinStartValue(theAlarm.low);
        rangeSeekbar.setMaxStartValue(theAlarm.high);
        rangeSeekbar.apply();

        rangeSeekbar.setOnRangeSeekbarFinalValueListener(new OnRangeSeekbarFinalValueListener() {
            @Override
            public void finalValue(Number minValue, Number maxValue) {
                Alarm theAlarm = null;
                for (Alarm alarm: alarms) {
                    if (alarm.type == viewTag) theAlarm = alarm;
                }
                if (theAlarm == null) {
                    theAlarm = new Alarm(maxValues[0], maxValues[1], viewTag, tagId);
                    alarms.add(theAlarm);
                }
                theAlarm.low = minValue.intValue();
                theAlarm.high = maxValue.intValue();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            save(null);
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
        // TODO: 13/09/17 do this in a non-stupid way
        for (Alarm alarm: alarms) {
            switch (alarm.type) {
                case Alarm.TEMPERATURE:
                    if (((CheckBox)findViewById(R.id.check_temp)).isChecked()) {
                        if (alarm.id == 0) alarm.save();
                        else alarm.update();
                    } else {
                        if (alarm.id != 0) alarm.delete();
                    }
                    break;
                case Alarm.HUMIDITY:
                    if (((CheckBox)findViewById(R.id.check_humi)).isChecked()) {
                        if (alarm.id == 0) alarm.save();
                        else alarm.update();
                    } else {
                        if (alarm.id != 0) alarm.delete();
                    }
                    break;
                case Alarm.PERSSURE:
                    if (((CheckBox)findViewById(R.id.check_pres)).isChecked()) {
                        if (alarm.id == 0) alarm.save();
                        else alarm.update();
                    } else {
                        if (alarm.id != 0) alarm.delete();
                    }
                    break;
                case Alarm.RSSI:
                    if (((CheckBox)findViewById(R.id.check_rssi)).isChecked()) {
                        if (alarm.id == 0) alarm.save();
                        else alarm.update();
                    } else {
                        if (alarm.id != 0) alarm.delete();
                    }
                    break;
            }
        }
        finish();
    }

    public String commaSeparate(Integer[] data) {
        if(data != null) {

            String separated = "";

            for (Integer i : data) {
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
