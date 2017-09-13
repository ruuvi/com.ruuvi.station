package com.ruuvi.tag.feature.list;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


import com.ruuvi.tag.R;
import com.ruuvi.tag.service.ScannerService;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.util.ComplexPreferences;
import com.ruuvi.tag.model.RuuviTagComplexList;
import com.ruuvi.tag.adapters.ListAdapter;

public class ListActivity extends AppCompatActivity {
    SharedPreferences savedTags;
    Gson gson;
    private ArrayList<RuuviTag> ruuviTagArrayList;
    private ListAdapter adapter;
    private Cursor cursor;
    private ListView beaconListView;
    private SharedPreferences settings;

    Timer timer;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ruuviTagArrayList = new ArrayList<>();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        beaconListView = (ListView)findViewById(R.id.listView);
        adapter = new ListAdapter(ruuviTagArrayList, this);
        beaconListView.setAdapter(adapter);

        savedTags = getSharedPreferences("saved_tags", MODE_PRIVATE);
        gson = new Gson();

        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ListActivity.this, ScannerService.class);
                RuuviTag tag = (RuuviTag) adapterView.getItemAtPosition(i);
                ScannerService.save(tag);
                startService(intent);
                finish();
            }
        });
    }

    private void setTimerForAdvertise() {
        timer = new Timer();
        TimerTask updateProfile = new CustomTimerTask();
        timer.scheduleAtFixedRate(updateProfile, 500, 500);
    }

    public class CustomTimerTask extends TimerTask {
        private Handler mHandler = new Handler();

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            importRuuviTags();
                        }
                    });
                }
            }).start();
        }
    }

    private void importRuuviTags() {
        ruuviTagArrayList.clear();
        ComplexPreferences complexPreferences = ComplexPreferences
                .getComplexPreferences(this, "saved_tags", MODE_PRIVATE);
        RuuviTagComplexList ruuvilist = complexPreferences.getObject("ruuvi", RuuviTagComplexList.class);

        if(ruuvilist != null) {
            for(RuuviTag ruuviTag : ruuvilist.ruuviTags) {
                ruuviTagArrayList.add(ruuviTag);
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        importRuuviTags();
        setTimerForAdvertise();
    }
}
