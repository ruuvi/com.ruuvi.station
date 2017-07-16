package fi.centria.ruuvitag.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import fi.centria.ruuvitag.R;

/**
 * Created by tmakinen on 27.6.2017.
 */

public class mainAdapter extends ArrayAdapter<Ruuvitag> implements View.OnClickListener {
    private ArrayList<Ruuvitag> ruuvitagArrayList;
    Context context;

    // View lookup cache
    private static class ViewHolder {
        TextView txtId;
        TextView txtRssi;
        TextView txtTemperature;
        TextView txtHumidity;
        TextView txtPressure;

    }

    public mainAdapter(ArrayList<Ruuvitag> ruuvitagArrayList, Context context) {
        super(context, R.layout.row_item_main, ruuvitagArrayList);
        this.ruuvitagArrayList = ruuvitagArrayList;
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
        Ruuvitag ruuvitag = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        mainAdapter.ViewHolder viewHolder;

        final View result;

        if(convertView == null) {
            viewHolder = new mainAdapter.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item_main, parent, false);
            viewHolder.txtId = (TextView) convertView.findViewById(R.id.id);
            viewHolder.txtRssi = (TextView) convertView.findViewById(R.id.rssi);
            viewHolder.txtTemperature = (TextView) convertView.findViewById(R.id.temperature);
            viewHolder.txtHumidity = (TextView) convertView.findViewById(R.id.humidity);
            viewHolder.txtPressure = (TextView) convertView.findViewById(R.id.pressure);

            result = convertView;
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (mainAdapter.ViewHolder) convertView.getTag();
            result = convertView;
        }

        viewHolder.txtId.setText(ruuvitag.getId());
        viewHolder.txtRssi.setText(ruuvitag.getRssi() + " dB");
        viewHolder.txtTemperature.setText(ruuvitag.getTemperature());
        viewHolder.txtHumidity.setText(ruuvitag.getHumidity());
        viewHolder.txtPressure.setText(ruuvitag.getPressure());

        // Return the completed view to render on screen
        return result;
    }
}
