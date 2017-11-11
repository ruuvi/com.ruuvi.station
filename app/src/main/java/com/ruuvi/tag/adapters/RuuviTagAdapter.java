package com.ruuvi.tag.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruuvi.tag.R;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.util.Utils;

import java.util.Date;
import java.util.List;

/**
 * Created by berg on 13/09/17.
 */

public class RuuviTagAdapter extends ArrayAdapter<RuuviTag> {
    private List<RuuviTag> tags;

    public RuuviTagAdapter(@NonNull Context context, List<RuuviTag> tags) {
        super(context, 0, tags);
        this.tags = tags;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final RuuviTag tag = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_main, parent, false);
        }

        TextView txtId = convertView.findViewById(R.id.id);
        TextView lastseen = convertView.findViewById(R.id.lastseen);
        TextView temp = convertView.findViewById(R.id.row_main_temperature);
        TextView humid = convertView.findViewById(R.id.row_main_humidity);
        TextView pres = convertView.findViewById(R.id.row_main_pressure);
        TextView signal = convertView.findViewById(R.id.row_main_signal);

        if(tag.name != null && !tag.name.isEmpty())
            txtId.setText(tag.name);
        else
            txtId.setText(tag.id);

        ((ImageView)convertView.findViewById(R.id.row_main_letter))
                .setImageBitmap(Utils.createBall((int)getContext().getResources().getDimension(R.dimen.letter_ball_radius),
                        getContext().getResources().getColor(R.color.main),
                        Color.WHITE,
                        txtId.getText().charAt(0) + ""));

        convertView.findViewById(R.id.row_main_root).setTag(tag);
        //convertView.findViewById(R.id.row_main_letter).setOnClickListener(tagMenuClickListener);

        String updatedAt = getContext().getResources().getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag.updateAt);

        lastseen.setText(updatedAt);

        temp.setText(String.format(getContext().getString(R.string.temperature_reading), tag.temperature));
        humid.setText(String.format(getContext().getString(R.string.humidity_reading), tag.humidity));
        pres.setText(String.format(getContext().getString(R.string.pressure_reading), tag.pressure));
        signal.setText(String.format(getContext().getString(R.string.signal_reading), tag.rssi));

        if (tag.url != null && !tag.url.isEmpty()) {
            convertView.findViewById(R.id.row_main_letter).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(tag.url));
                    getContext().startActivity(intent);
                }
            });
        }

        return convertView;
    }
}
