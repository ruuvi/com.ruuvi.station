package com.ruuvi.tag.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruuvi.tag.R;
import com.ruuvi.tag.feature.edit.EditActivity;
import com.ruuvi.tag.feature.plot.PlotActivity;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.util.Utils;

import java.util.Date;
import java.util.List;

/**
 * Created by berg on 13/09/17.
 */

public class RuuviTagAdapter extends ArrayAdapter<RuuviTag> {
    public RuuviTagAdapter(@NonNull Context context, List<RuuviTag> tags) {
        super(context, 0, tags);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final RuuviTag tag = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_main, parent, false);
        }

        TextView txtId = convertView.findViewById(R.id.id);
        TextView txtRssi = convertView.findViewById(R.id.rssi);
        TextView txtTemperature = convertView.findViewById(R.id.temperature);
        TextView txtHumidity = convertView.findViewById(R.id.humidity);
        TextView txtPressure = convertView.findViewById(R.id.pressure);
        TextView txtLast = convertView.findViewById(R.id.lastseen);
        ImageButton edit = convertView.findViewById(R.id.edit);
        ImageButton www = convertView.findViewById(R.id.openInBrowser);

        if(tag.name != null && !tag.name.isEmpty())
            txtId.setText(tag.name);
        else
            txtId.setText(tag.id);

        txtRssi.setText(tag.rssi + " dB");
        txtTemperature.setText(tag.temperature + "°C" + " / " + tag.getFahrenheit() + "°F");
        txtHumidity.setText(tag.humidity + "%");
        txtPressure.setText(tag.pressure + " hPa");

        Date dateNow = new Date();
        long diffInMS = dateNow.getTime() - tag.updateAt.getTime();
        txtLast.setText(tag.updateAt.toLocaleString() + " / " + (tag.url != null ? tag.url : "RawMode"));

        if(diffInMS > 1000*60)
            txtLast.setTextColor(Color.RED);
        else
            txtLast.setTextColor(getContext().getResources().getColor(R.color.ap_gray));

        convertView.findViewById(R.id.openInBrowser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = tag.id;

                Intent intent = new Intent(getContext(), PlotActivity.class);
                intent.putExtra("id", id);
                getContext().startActivity(intent);

                /*
                String url = tag.url;
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(tag.url));
                getContext().startActivity(intent);
                */
            }
        });

        convertView.findViewById(R.id.edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), EditActivity.class);
                intent.putExtra("id", tag.id);
                getContext().startActivity(intent);
            }
        });

        ((ImageView)convertView.findViewById(R.id.row_main_letter))
                .setImageBitmap(Utils.createBall(100,
                        R.color.ap_gray,
                        Color.WHITE,
                        txtId.getText().charAt(0) + ""));
        return convertView;
    }
}
