package fi.centria.ruuvitag;

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
import java.util.Timer;
import java.util.TimerTask;

import fi.centria.ruuvitag.database.DBContract;
import fi.centria.ruuvitag.database.DBHandler;
import fi.centria.ruuvitag.util.ComplexPreferences;
import fi.centria.ruuvitag.util.Ruuvitag;
import fi.centria.ruuvitag.util.RuuvitagComplexList;
import fi.centria.ruuvitag.util.listAdapter;

public class ListActivity extends AppCompatActivity {
    SharedPreferences savedTags;
    Gson gson;
    private ArrayList<Ruuvitag> ruuvitagArrayList;
    private listAdapter adapter;
    private Cursor cursor;
    private ListView beaconListView;
    private SharedPreferences settings;

    Timer timer;
    SQLiteDatabase db;
    DBHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ruuvitagArrayList = new ArrayList<>();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        handler = new DBHandler(getApplicationContext());
        db = handler.getReadableDatabase();

        cursor = db.rawQuery("SELECT * FROM " + DBContract.RuuvitagDB.TABLE_NAME, null);
        beaconListView = (ListView)findViewById(R.id.listView);
        adapter = new listAdapter(ruuvitagArrayList, this);
        beaconListView.setAdapter(adapter);

        savedTags = getSharedPreferences("saved_tags", MODE_PRIVATE);
        gson = new Gson();

        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ListActivity.this, ScannerService.class);
                Ruuvitag temp = (Ruuvitag) adapterView.getItemAtPosition(i);
                intent.putExtra("favorite", temp);
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
                            importRuuvitags();
                        }
                    });
                }
            }).start();
        }
    }

    private void importRuuvitags() {
        ruuvitagArrayList.clear();
        ComplexPreferences complexPreferences = ComplexPreferences
                .getComplexPreferences(this, "saved_tags", MODE_PRIVATE);
        RuuvitagComplexList ruuvilist = complexPreferences.getObject("ruuvi", RuuvitagComplexList.class);
        for(Ruuvitag ruuvitag : ruuvilist.ruuvitags) {
            ruuvitagArrayList.add(ruuvitag);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        importRuuvitags();
        setTimerForAdvertise();
    }
}
