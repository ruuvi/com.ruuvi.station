package com.ruuvi.station.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruuvi.station.R;
import com.ruuvi.station.database.RuuviTagRepository;
import com.ruuvi.station.model.RuuviTagEntity;
import com.ruuvi.station.util.AlarmChecker;
import com.ruuvi.station.util.Utils;

import java.util.List;

/**
 * Created by berg on 13/09/17.
 */

public class RuuviTagAdapter extends ArrayAdapter<RuuviTagEntity> {

    private List<RuuviTagEntity> tags;

    public RuuviTagAdapter(@NonNull Context context, List<RuuviTagEntity> tags) {
        super(context, 0, tags);
        this.tags = tags;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final RuuviTagEntity tag = getItem(position);


        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_main, parent, false);
        }

        TextView txtId = convertView.findViewById(R.id.id);
        TextView lastseen = convertView.findViewById(R.id.lastseen);
        TextView temp = convertView.findViewById(R.id.row_main_temperature);
        TextView humid = convertView.findViewById(R.id.row_main_humidity);
        TextView pres = convertView.findViewById(R.id.row_main_pressure);
        TextView signal = convertView.findViewById(R.id.row_main_signal);

        txtId.setText(tag.getDisplayName());

        int ballColorRes = (position % 2 == 0) ? R.color.main : R.color.mainLight;

        ((ImageView)convertView.findViewById(R.id.row_main_letter))
                .setImageBitmap(Utils.createBall((int)getContext().getResources().getDimension(R.dimen.letter_ball_radius),
                        getContext().getResources().getColor(ballColorRes),
                        Color.WHITE,
                        txtId.getText().charAt(0) + ""));

        convertView.findViewById(R.id.row_main_root).setTag(tag);
        //convertView.findViewById(R.id.row_main_letter).setOnClickListener(tagMenuClickListener);

        String updatedAt = getContext().getResources().getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag.getUpdateAt());

        lastseen.setText(updatedAt);
        AppCompatImageView bell = convertView.findViewById(R.id.bell);
        int status = AlarmChecker.getStatus(tag);
        switch (status) {
            case -1:
                bell.setVisibility(View.VISIBLE);
                bell.setImageResource(R.drawable.ic_notifications_off_24px);
                break;
            case 0:
                bell.setVisibility(View.VISIBLE);
                bell.setImageResource(R.drawable.ic_notifications_on_24px);
                break;
            case 1:
                bell.setImageResource(R.drawable.ic_notifications_active_24px);
                bell.setVisibility(bell.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                break;
        }
        ImageViewCompat.setImageTintList(bell, ColorStateList.valueOf(getContext().getResources().getColor(R.color.main)));

        temp.setText(RuuviTagRepository.getTemperatureString(getContext(), tag));
        humid.setText(String.format(getContext().getString(R.string.humidity_reading), tag.getHumidity()));
        pres.setText(String.format(getContext().getString(R.string.pressure_reading), tag.getPressure() / 100.0));
        signal.setText(String.format(getContext().getString(R.string.signal_reading), tag.getRssi()));

        return convertView;
    }
}
