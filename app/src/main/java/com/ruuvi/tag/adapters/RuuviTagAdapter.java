package com.ruuvi.tag.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.tag.R;
import com.ruuvi.tag.feature.edit.AlarmEditActivity;
import com.ruuvi.tag.feature.edit.EditActivity;
import com.ruuvi.tag.feature.plot.PlotActivity;
import com.ruuvi.tag.model.Alarm;
import com.ruuvi.tag.model.Alarm_Table;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.model.TagSensorReading;
import com.ruuvi.tag.model.TagSensorReading_Table;
import com.ruuvi.tag.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
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
        TextView sensorValues = convertView.findViewById(R.id.sensorValues);
        TextView alertsRssi = convertView.findViewById(R.id.alerts_rssi);
        AppCompatImageButton menuButton = convertView.findViewById(R.id.edit);

        menuButton.setTag(tag);
        menuButton.setOnClickListener(tagMenuClickListener);

        if(tag.name != null && !tag.name.isEmpty())
            txtId.setText(tag.name);
        else
            txtId.setText(tag.id);

        ((ImageView)convertView.findViewById(R.id.row_main_letter))
                .setImageBitmap(Utils.createBall(100,
                        R.color.ap_gray,
                        Color.WHITE,
                        txtId.getText().charAt(0) + ""));

        Date dateNow = new Date();
        long diffInMS = dateNow.getTime() - tag.updateAt.getTime();
        String updated = getContext().getResources().getString(R.string.updated) + " ";
        // show date if the tag has not been seen for 24h
        if (diffInMS > 24 * 60 * 60 * 1000) {
            updated += tag.updateAt.toString();
        } else {
            int seconds = (int) (diffInMS / 1000) % 60 ;
            int minutes = (int) ((diffInMS / (1000*60)) % 60);
            int hours   = (int) ((diffInMS / (1000*60*60)) % 24);
            if (hours > 0) updated += hours + " h ";
            if (minutes > 0) updated += minutes + " min ";
            updated += seconds + " s ago";
        }

        lastseen.setText(updated);

        /*
        if(diffInMS > 1000*60)
            txtLast.setTextColor(Color.RED);
        else
            txtLast.setTextColor(getContext().getResources().getColor(R.color.ap_gray));
        */

        String alertStr = getContext().getResources().getString(R.string.alerts) + ": ";
        long alarmCount = SQLite.selectCountOf().from(Alarm.class).where(Alarm_Table.ruuviTagId.eq(tag.id))
                .count();
        alertStr += alarmCount > 0 ? "On" : "Off";
        alertStr += " / " + getContext().getResources().getString(R.string.signal) + ": ";
        alertStr += tag.rssi + " dB";

        alertsRssi.setText(alertStr);

        String sensorStr = tag.temperature + "°" + " / " +
                tag.pressure + " hPa / " +
                tag.humidity + "% RH";
        if (tag.voltage > 0) sensorStr += " / " + tag.voltage + " V";

        sensorValues.setText(sensorStr);

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

    View.OnClickListener tagMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final RuuviTag tag = (RuuviTag)v.getTag();

            final BottomSheetDialog dialog = new BottomSheetDialog(getContext());

            ListView listView = new ListView(getContext());

            List<String> menu = new ArrayList<>(Arrays.asList(getContext().getResources().getStringArray(R.array.station_tag_menu)));

            if (tag.url != null && !tag.url.isEmpty()) {
                menu.add(getContext().getResources().getString(R.string.share));
            }

            listView.setAdapter(
                    new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_list_item_1,
                            menu
                    )
            );

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i == 0) {
                        Toast.makeText(getContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
                    } else if (i == 1) {
                        Intent intent = new Intent(getContext(), AlarmEditActivity.class);
                        intent.putExtra("tagId", tag.id);
                        getContext().startActivity(intent);
                    } else if (i == 2) {
                        Intent intent = new Intent(getContext(), EditActivity.class);
                        intent.putExtra("id", tag.id);
                        getContext().startActivity(intent);
                    } else if (i == 3) {
                        tags.remove(tag);
                        tag.deleteTagAndRelatives();
                    } else if (i == 4) {
                        Intent intent = new Intent(getContext(), PlotActivity.class);
                        intent.putExtra("id", tag.id);
                        getContext().startActivity(intent);
                    } else if (i == 5) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
                        intent.putExtra(Intent.EXTRA_TEXT, tag.url);
                        getContext().startActivity(Intent.createChooser(intent, "Share URL"));
                    }

                    dialog.dismiss();
                }
            });

            dialog.setContentView(listView);
            dialog.show();
        }
    };
}
