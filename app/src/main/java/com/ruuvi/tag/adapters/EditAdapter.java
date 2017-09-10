package com.ruuvi.tag.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


import com.ruuvi.tag.R;
import com.ruuvi.tag.model.Alarm;

/**
 * Created by tmakinen on 26.7.2017.
 */

public class EditAdapter extends ArrayAdapter<Alarm> {
    public EditAdapter(Context context, ArrayList<Alarm> alarms) {
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
        type.setText(alarm.type);
        range.setText("Normal range: " + String.valueOf(alarm.low) + " - " + String.valueOf(alarm.high));
        return convertView;
    }
}
