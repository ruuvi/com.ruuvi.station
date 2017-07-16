package fi.centria.ruuvitag;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import fi.centria.ruuvitag.database.AndroidDatabaseManager;
import fi.centria.ruuvitag.database.DBAdapter;
import fi.centria.ruuvitag.database.DBContract;
import fi.centria.ruuvitag.database.DBHandler;
import fi.centria.ruuvitag.settings.SettingsActivity;
import fi.centria.ruuvitag.util.DeviceIdentifier;

public class MainActivity extends AppCompatActivity {
    ScannerService service;
    private DBAdapter adapter;
    private ListView beaconListView;
    private Gson gson;
    private Timer timer;
    private Cursor cursor;
    private SQLiteDatabase db;
    private DBHandler handler;
    private View text;
    private boolean bound;
    private SharedPreferences settings;

    public void openList(View view) {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    public void editRuuvitag(View view) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("index", (Integer) view.getTag());
        startActivity(intent);
    }

    public void openRuuviInBrowser(View v)
    {

        int index =  (Integer) v.getTag();


        cursor = db.query(DBContract.RuuvitagDB.TABLE_NAME, null, "_ID= ?", new String[] { "" + index }, null, null, null);
        if(cursor != null)
            cursor.moveToFirst();

        String url  = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_URL));
        String id  = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_ID));
        Intent intent = new Intent(this, PlotActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);

        /*
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
        startActivity(intent);*/
    }

    private void setTimerForAdvertise() {
        timer = new Timer();
        TimerTask updateProfile = new MainActivity.CustomTimerTask();
        timer.scheduleAtFixedRate(updateProfile, 0, 1000);
    }

    private class CustomTimerTask extends TimerTask {
        private Handler mHandler = new Handler();

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cursor = db.rawQuery("SELECT * FROM " + DBContract.RuuvitagDB.TABLE_NAME, null);
                            adapter.changeCursor(cursor);
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gson = new Gson();
        text = findViewById(R.id.noTags_textView);

        DeviceIdentifier.id(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        handler = new DBHandler(this);
        db = handler.getReadableDatabase();
        cursor = db.rawQuery("SELECT * FROM " + DBContract.RuuvitagDB.TABLE_NAME, null);

        beaconListView = (ListView) findViewById(R.id.Tags_listView);
        adapter = new DBAdapter(this, cursor, 0);
        beaconListView.setAdapter(adapter);

        setTitle(R.string.title_activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openList(view);
            }
        });

        Button button =(Button) findViewById(R.id.yourbuttonid);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent dbmanager = new Intent(MainActivity.this, AndroidDatabaseManager.class);
                startActivity(dbmanager);
            }
        });

        setTimerForAdvertise();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        Intent intent = new Intent(MainActivity.this, ScannerService.class);
        startService(intent);
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
        }

        cursor = db.rawQuery("SELECT * FROM " + DBContract.RuuvitagDB.TABLE_NAME, null);
        adapter.changeCursor(cursor);
        text.setVisibility((adapter.isEmpty())?View.VISIBLE:View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}

