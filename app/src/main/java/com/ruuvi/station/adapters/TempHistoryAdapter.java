package com.ruuvi.station.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ruuvi.station.R;
import com.ruuvi.station.model.HistoryItem;

import java.text.SimpleDateFormat;
import java.util.List;

public class TempHistoryAdapter extends ArrayAdapter<HistoryItem> {
    private List<HistoryItem> tags;

    public TempHistoryAdapter(@NonNull Context context, List<HistoryItem> tags) {
        super(context, 0, tags);
        this.tags = tags;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final HistoryItem tag = getItem(position);


        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_temp, parent, false);
        }

        TextView date = convertView.findViewById(R.id.date);
        TextView min = convertView.findViewById(R.id.min);
        TextView max = convertView.findViewById(R.id.max);

        date.setText(tag.date.toString());

        convertView.findViewById(R.id.row_temp_root).setTag(tag);

        SimpleDateFormat formatDate = new SimpleDateFormat("dd.MM.yyyy");

        date.setText(formatDate.format(tag.date));
        min.setText(Double.toString(tag.minTemp) + tag.unit);
        max.setText(Double.toString(tag.maxTemp) + tag.unit);

        return convertView;
    }
}
