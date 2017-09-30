package com.ruuvi.tag.feature.main;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.tag.R;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.service.BackgroundScanner;
import com.ruuvi.tag.util.DataUpdateListener;
import com.ruuvi.tag.util.RuuviTagListener;
import com.ruuvi.tag.util.RuuviTagScanner;

public class MainActivity extends AppCompatActivity implements RuuviTagListener {
    private DrawerLayout drawerLayout;
    private RuuviTagScanner scanner;
    public List<RuuviTag> myRuuviTags = new ArrayList<>();
    private DataUpdateListener fragmentWithCallback;
    private Handler handler;

    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            if (fragmentWithCallback != null) fragmentWithCallback.dataUpdated();
            handler.postDelayed(updater, 1000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main_drawerLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handler = new Handler();

        myRuuviTags = RuuviTag.getAll();
        scanner = new RuuviTagScanner(this, getApplicationContext());

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );

        drawerLayout.addDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        drawerToggle.syncState();

        ListView drawerListView = findViewById(R.id.navigationDrawer_listView);

        drawerListView.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        getResources().getStringArray(R.array.navigation_items)
                )
        );

        drawerListView.setOnItemClickListener(drawerItemClicked);

        boolean alarmUp = (PendingIntent.getBroadcast(this, 0,
                new Intent("com.ruuvi.tag.service.BackgroundScanner"),
                PendingIntent.FLAG_NO_CREATE) != null);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean("pref_bgscan", false) && !alarmUp) {

            int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "5")) * 1000;

            Intent i = new Intent(this, BackgroundScanner.class);

            PendingIntent sender = PendingIntent.getBroadcast(this, BackgroundScanner.REQUEST_CODE, i, 0);

            AlarmManager am = (AlarmManager) this
                    .getSystemService(ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                    scanInterval, sender);
        }

        openFragment(0);
    }

    AdapterView.OnItemClickListener drawerItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            openFragment(i);
        }
    };

    @Override
    protected void onStart() {
        //Intent intent = new Intent(MainActivity.this, ScannerService.class);
        //startService(intent);
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Permission check for Marshmallow and newer
        int permissionCoarseLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        int permissionWriteExternal = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if(permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if(permissionWriteExternal != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
        } else {
            myRuuviTags.clear();
            myRuuviTags.addAll(RuuviTag.getAll());
            scanner.start();
            handler.post(updater);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stop();
        handler.removeCallbacks(updater);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    private void openFragment(int type) {
        Fragment fragment = null;
        switch (type) {
            case 1:
                fragment = new SettingsFragment();
                fragmentWithCallback = null;
                break;
            case 2:
                fragment = new AboutFragment();
                fragmentWithCallback = null;
                break;
            default:
                fragment = new RuuviStationFragment();
                fragmentWithCallback = (DataUpdateListener)fragment;
                type = 0;
                break;
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.main_contentFrame, fragment)
                .commit();
        setTitle(getResources().getStringArray(R.array.navigation_items)[type]);
        drawerLayout.closeDrawers();
    }

    @Override
    public void tagFound(RuuviTag tag) {
        for (RuuviTag myTag: myRuuviTags) {
            if (myTag.id.equals(tag.id)) {
                myTag.updateDataFrom(tag);
                if (fragmentWithCallback != null) fragmentWithCallback.dataUpdated();
            }
        }
    }
}

