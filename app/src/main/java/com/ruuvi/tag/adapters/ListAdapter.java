package com.ruuvi.tag.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


import com.ruuvi.tag.R;
import com.ruuvi.tag.model.RuuviTag;

/**
 * Created by tmakinen on 21.6.2017.
 */

public class ListAdapter extends ArrayAdapter<RuuviTag> implements View.OnClickListener {
    private ArrayList<RuuviTag> ruuviTagArrayList;
    Context context;

    // View lookup cache
    private static class ViewHolder {
        TextView txtId;
        TextView txtUrl;
        TextView txtRssi;
        ImageView select;
    }

    public ListAdapter(ArrayList<RuuviTag> ruuviTagArrayList, Context context) {
        super(context, R.layout.row_item_list, ruuviTagArrayList);
        this.ruuviTagArrayList = ruuviTagArrayList;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        Log.d("tagi", "nappia painettu");
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        RuuviTag ruuviTag = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder;

        final View result;

        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item_list, parent, false);
            viewHolder.txtId = (TextView) convertView.findViewById(R.id.id);
            viewHolder.txtUrl = (TextView) convertView.findViewById(R.id.url);
            viewHolder.txtRssi = (TextView) convertView.findViewById(R.id.rssi);

            result = convertView;
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        RuuviTag existingTag = RuuviTag.get(ruuviTag.id);
        String tagId = ruuviTag.id;
        if (existingTag != null) {
            result.setBackgroundColor(Color.LTGRAY);
            if (existingTag.name != null && !existingTag.name.isEmpty()) {
                tagId = tagId + " (" + existingTag.name + ")";
            } else {
                result.setBackgroundColor(Color.WHITE);
            }
        }

        viewHolder.txtId.setText(tagId);
        viewHolder.txtUrl.setText(ruuviTag.url);
        viewHolder.txtRssi.setText(ruuviTag.rssi + " dB");


        // Return the completed view to render on screen
        return result;
    }
}
