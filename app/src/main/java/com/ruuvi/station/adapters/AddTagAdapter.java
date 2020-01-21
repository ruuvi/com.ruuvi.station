package com.ruuvi.station.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ruuvi.station.R;
import com.ruuvi.station.bluetooth.domain.IRuuviTag;
import com.ruuvi.station.model.RuuviTag;

import java.util.List;

/**
 * Created by berg on 10/10/17.
 */

public class AddTagAdapter extends ArrayAdapter<IRuuviTag> {

    private List<IRuuviTag> tags;

    public AddTagAdapter(@NonNull Context context, List<IRuuviTag> tags) {
        super(context, 0, tags);
        this.tags = tags;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final IRuuviTag tag = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_add, parent, false);
        }

        ((TextView)convertView.findViewById(R.id.address)).setText(tag.getId());
        ((TextView)convertView.findViewById(R.id.rssi)).setText(String.format(getContext().getResources().getString(R.string.signal_reading), tag.getRssi()));

        AppCompatImageView signalIcon = convertView.findViewById(R.id.signalIcon);
        if (tag.getRssi() < -80) signalIcon.setImageResource(R.drawable.icon_connection_1);
        else if (tag.getRssi() < -50) signalIcon.setImageResource(R.drawable.icon_connection_2);
        else signalIcon.setImageResource(R.drawable.icon_connection_3);

        return convertView;
    }
}
