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
import android.widget.Toast;

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
import com.ruuvi.tag.util.RuuviTagListener;
import com.ruuvi.tag.util.RuuviTagScanner;

public class ListActivity extends AppCompatActivity implements RuuviTagListener {
    SharedPreferences savedTags;
    Gson gson;
    private ArrayList<RuuviTag> ruuviTagArrayList;
    private ListAdapter adapter;
    private ListView beaconListView;
    private SharedPreferences settings;
    private RuuviTagScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ruuviTagArrayList = new ArrayList<>();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        beaconListView = (ListView)findViewById(R.id.listView);
        adapter = new ListAdapter(ruuviTagArrayList, this);
        beaconListView.setAdapter(adapter);

        scanner = new RuuviTagScanner(this, getApplicationContext());

        savedTags = getSharedPreferences("saved_tags", MODE_PRIVATE);
        gson = new Gson();

        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RuuviTag tag = (RuuviTag) adapterView.getItemAtPosition(i);

                if (RuuviTag.get(tag.id) != null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.tag_already_added), Toast.LENGTH_SHORT)
                        .show();
                    return;
                }
                Intent intent = new Intent(ListActivity.this, ScannerService.class);
                tag.save();
                ScannerService.logTag(tag);
                startService(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanner.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stop();
    }

    @Override
    public void tagFound(RuuviTag tag) {
        int found = -1;
        for (int i = 0; i < ruuviTagArrayList.size(); i++) {
            if (ruuviTagArrayList.get(i).id.equals(tag.id)) found = i;
        }
        if (found > -1) ruuviTagArrayList.set(found, tag);
        else ruuviTagArrayList.add(tag);

        adapter.notifyDataSetChanged();
    }
}
