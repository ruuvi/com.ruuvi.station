package com.ruuvi.station.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;


import com.ruuvi.station.R;
import com.ruuvi.station.model.Alarm;

/**
 * Created by tmakinen on 26.7.2017.
 */

public class EditAdapter extends ArrayAdapter<Alarm> {
    public EditAdapter(Context context, List<Alarm> alarms) {
        super(context, 0, alarms);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Alarm alarm = getItem(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_alarm, parent, false);
        }
        TextView type = (TextView) convertView.findViewById(R.id.alarm_name);
        TextView range = (TextView) convertView.findViewById(R.id.alarm_range);
        type.setText(alarm.type == Alarm.TEMPERATURE ? getContext().getString(R.string.temperature) :
                        alarm.type == Alarm.HUMIDITY ? getContext().getString(R.string.humidity) :
                        alarm.type == Alarm.PERSSURE ? getContext().getString(R.string.pressure) :
                        getContext().getString(R.string.rssi)
        );
        range.setText("Normal range: " + String.valueOf(alarm.low) + " - " + String.valueOf(alarm.high));
        return convertView;
    }
}
