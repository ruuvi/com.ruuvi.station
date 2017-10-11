package com.ruuvi.tag.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ruuvi.tag.R;
import com.ruuvi.tag.feature.main.MainActivity;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.service.ScannerService;

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
        ((TextView)convertView.findViewById(R.id.rssi)).setText(tag.rssi + " dB");

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
