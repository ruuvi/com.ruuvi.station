package com.ruuvi.tag.feature;

import android.content.DialogInterface;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.CharacterPickerDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.ruuvi.tag.R;
import com.ruuvi.tag.model.Alarm;
import com.ruuvi.tag.model.RuuviTag;

import java.util.ArrayList;
import java.util.List;

public class TagSettings extends AppCompatActivity {
    public static final String TAG_ID = "TAG_ID";

    private RuuviTag tag;
    List<Alarm> tagAlarms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        String tagId = getIntent().getStringExtra(TAG_ID);

        tag = RuuviTag.get(tagId);
        if (tag == null) {
            finish();
            return;
        }
        tagAlarms = Alarm.getForTag(tagId);

        final TextView nameTextView = findViewById(R.id.input_name);
        nameTextView.setText((tag.name != null && !tag.name.isEmpty()) ? tag.name : tag.id);

        final TextView gatewayTextView = findViewById(R.id.input_gatewayUrl);
        if (tag.gatewayUrl != null && !tag.gatewayUrl.isEmpty()) gatewayTextView.setText(tag.gatewayUrl);

        // TODO: 25/10/17 make this less ugly
        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TagSettings.this, R.style.Theme_AppCompat_Light_Dialog);
                builder.setTitle(getString(R.string.tag_name));
                final EditText input = new EditText(TagSettings.this);
                input.setText(tag.name);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tag.name = input.getText().toString();
                        nameTextView.setText(tag.name);
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });

        gatewayTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TagSettings.this, R.style.Theme_AppCompat_Light_Dialog);
                builder.setTitle(getString(R.string.gateway_url));
                final EditText input = new EditText(TagSettings.this);
                input.setText(tag.gatewayUrl);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tag.gatewayUrl = input.getText().toString();
                        gatewayTextView.setText(!tag.gatewayUrl.isEmpty() ? tag.gatewayUrl : getString(R.string.no_gateway_url));
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });


        List<AlarmItem> alarmItems = new ArrayList<>();
        alarmItems.add(new AlarmItem(getString(R.string.temperature), getString(R.string.alert_subtitle_off), Alarm.TEMPERATURE, false, -40, 85));
        alarmItems.add(new AlarmItem(getString(R.string.humidity), getString(R.string.alert_subtitle_off), Alarm.HUMIDITY, false, 0, 100));
        alarmItems.add(new AlarmItem(getString(R.string.pressure), getString(R.string.alert_subtitle_off), Alarm.PERSSURE, false, 300, 1100));
        alarmItems.add(new AlarmItem(getString(R.string.rssi), getString(R.string.alert_subtitle_off), Alarm.RSSI, false, -100 ,0));

        for (Alarm alarm: tagAlarms) {
            AlarmItem item = alarmItems.get(alarm.type);
            item.high = alarm.high;
            item.low = alarm.low;
            item.checked = true;
            item.alarmId = alarm.id;
        }

        LayoutInflater inflater = getLayoutInflater();
        ConstraintLayout parentLayout = findViewById(R.id.tag_settings_item_layout);
        TextView alertsHeader = findViewById(R.id.alerts_header);

        for (int i = 0; i < alarmItems.size(); i++) {
            AlarmItem item = alarmItems.get(i);
            item.view = inflater.inflate(R.layout.view_alarm, parentLayout, false);
            item.view.setId(item.view.getId() + i);
            item.updateView();
            parentLayout.addView(item.view);
            ConstraintSet set = new ConstraintSet();
            set.clone(parentLayout);
            set.connect(item.view.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            set.connect(item.view.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            set.connect(item.view.getId(), ConstraintSet.TOP, (i == 0 ? alertsHeader.getId() : alarmItems.get(i - 1).view.getId()), ConstraintSet.BOTTOM);
            set.applyTo(parentLayout);
        }
    }

    private class AlarmItem {
        public String name;
        public String subtitle;
        public boolean checked;
        public int low;
        public int high;
        public int max;
        public int min;
        public int type;
        public View view;
        public int alarmId;

        public AlarmItem(String name, String subtitle, int type, boolean checked, int max, int min) {
            this.name = name;
            this.subtitle = subtitle;
            this.type = type;
            this.checked = checked;
            this.min = min;
            this.max = max;
            this.low = min;
            this.high = max;
        }

        public void updateView() {
            CrystalRangeSeekbar seekBar = this.view.findViewById(R.id.alert_seekBar);
            seekBar.setMinValue(this.min);
            seekBar.setMaxValue(this.max);
            if (this.checked) {
                seekBar.setLeft(this.low);
                seekBar.setRight(this.high);
                seekBar.setBarHighlightColor(getResources().getColor(R.color.accentDark));
                seekBar.setLeftThumbColor(getResources().getColor(R.color.accentDark));
                seekBar.setRightThumbColor(getResources().getColor(R.color.accentDark));
            } else {
                seekBar.setLeft(this.min);
                seekBar.setRight(this.max);
                seekBar.setEnabled(false);
                seekBar.setBarHighlightColor(getResources().getColor(R.color.ap_gray));
                seekBar.setLeftThumbColor(getResources().getColor(R.color.ap_gray));
                seekBar.setRightThumbColor(getResources().getColor(R.color.ap_gray));
            }
            ((CheckBox)this.view.findViewById(R.id.alert_checkbox)).setChecked(this.checked);
            ((TextView)this.view.findViewById(R.id.alert_title)).setText(this.name);
            ((TextView)this.view.findViewById(R.id.alert_subtitle)).setText(this.subtitle);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            tag.update();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }
}
