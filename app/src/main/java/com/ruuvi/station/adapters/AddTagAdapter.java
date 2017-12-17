package com.ruuvi.station.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ruuvi.station.R;
import com.ruuvi.station.feature.main.MainActivity;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.service.ScannerService;

import java.util.List;

/**
 * Created by berg on 10/10/17.
 */

public class AddTagAdapter extends ArrayAdapter<RuuviTag> {
    private List<RuuviTag> tags;

    public AddTagAdapter(@NonNull Context context, List<RuuviTag> tags) {
        super(context, 0, tags);
        this.tags = tags;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final RuuviTag tag = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_add, parent, false);
        }

        ((TextView)convertView.findViewById(R.id.address)).setText(tag.id);
        ((TextView)convertView.findViewById(R.id.rssi)).setText(String.format(getContext().getResources().getString(R.string.signal_reading), tag.rssi));

        AppCompatImageView signalIcon = convertView.findViewById(R.id.signalIcon);
        if (tag.rssi < -80) signalIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.icon_connection_1));
        else if (tag.rssi < -50) signalIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.icon_connection_2));
        else signalIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.icon_connection_3));

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RuuviTag.get(tag.id) != null) {
                    Toast.makeText(getContext(), getContext().getString(R.string.tag_already_added), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                tag.save();
                ScannerService.logTag(tag);
                ((MainActivity)getContext()).openFragment(1);
            }
        });
        return convertView;
    }
}
