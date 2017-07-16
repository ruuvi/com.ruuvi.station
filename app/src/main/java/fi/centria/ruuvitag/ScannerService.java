package fi.centria.ruuvitag;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import fi.centria.ruuvitag.database.DBContract;
import fi.centria.ruuvitag.database.DBHandler;
import fi.centria.ruuvitag.model.ScanEvent;
import fi.centria.ruuvitag.util.ComplexPreferences;
import fi.centria.ruuvitag.util.DeviceIdentifier;
import fi.centria.ruuvitag.util.Foreground;
import fi.centria.ruuvitag.util.PlotSource;
import fi.centria.ruuvitag.util.Ruuvitag;
import fi.centria.ruuvitag.util.RuuvitagComplexList;

public class ScannerService extends Service implements BeaconConsumer {
    List<Ruuvitag> ruuvitagArrayList;
    private BeaconManager beaconManager;
    private BackgroundPowerSaver bps;
    SharedPreferences settings;
    DBHandler handler;
    SQLiteDatabase db;
    Cursor cursor;
    Region region;
    private Calendar c;
    private String backendUrl;
    private PlotSource plotSource;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle data = intent.getExtras();
        if(data != null) {
            save((Ruuvitag) data.getParcelable("favorite"));
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(mListener);

        if(settings.getBoolean("pref_bgscan", false))
            startFG();

        Foreground.init(getApplication());
        Foreground.get().addListener(listener);

        bps = new BackgroundPowerSaver(this);

        ruuvitagArrayList = new ArrayList<>();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        handler = new DBHandler(getApplicationContext());

        // Detect the URL frame:
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));

        beaconManager.bind(this);

        backendUrl = settings.getString("pref_backend",null);

        db = handler.getWritableDatabase();

        plotSource =  PlotSource.getInstance();
    }


    public SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(settings.getBoolean("pref_bgscan", false))
                startFG();

            if(!settings.getBoolean("pref_bgscan", false))
                stopForeground(true);


                backendUrl = settings.getString("pref_backend",null);

            try {
                beaconManager.setForegroundBetweenScanPeriod
                        (Long.parseLong(settings.getString("pref_scaninterval", "1")) * 1000 - 1000l);
                beaconManager.updateScanPeriods();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {
                ruuvitagArrayList.clear();

                ScanEvent scanEvent = new ScanEvent(getApplicationContext(), DeviceIdentifier.id(getApplicationContext()));


                for(Beacon beacon : beacons) {
                    if(beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10)
                    {
                        // Parse url from beacon advert
                        String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                        if (url.startsWith("https://ruu.vi/#") || url.startsWith("https://r/")) {
                            // Creates temporary ruuvitag-object, without heavy calculations
                            Ruuvitag temp = new Ruuvitag(beacon, true);
                            if(checkForSameTag(temp))
                            {
                                // Creates real object, with temperature etc. calculated
                                Ruuvitag real = new Ruuvitag(beacon, false);
                                ruuvitagArrayList.add(real);
                                update(real);


                                scanEvent.addRuuvitag(real);
                            }
                        }
                    }
                }

                if(backendUrl != null)
                {
                    //JsonObject json = new JsonObject();
                  //  JsonObject json = JSON.(scanEvent);
                   String jsonData =  new Gson().toJson(scanEvent);
                    Ion.with(getApplicationContext())
                            .load(backendUrl)
                            .setJsonPojoBody(scanEvent)
                            .asJsonObject()
                            .setCallback(new FutureCallback<JsonObject>() {
                                @Override
                                public void onCompleted(Exception e, JsonObject result) {
                                    // do stuff with the result or error
                                }
                            });
                }
                plotSource.addScanEvent(scanEvent);

                exportRuuvitags();
                exportDB();
            }
        });

        Random r = new Random();
        region = new Region("RuuvitagRegion_"+r.nextInt(1000), null, null, null);

        beaconManager.setForegroundScanPeriod(1000l);
        beaconManager.setBackgroundBetweenScanPeriod(60000l);
        beaconManager.setBackgroundScanPeriod(1000l);

        try {
            beaconManager.setForegroundBetweenScanPeriod
                    (Long.parseLong(settings.getString("pref_scaninterval", "5")) * 1000 - 1000l);
            beaconManager.updateScanPeriods();
            this.beaconManager.startRangingBeaconsInRegion(region);
        }  catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startFG() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notification;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        notification
                = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(this.getString(R.string.scanner_notification_title))
                .setSmallIcon(R.mipmap.ic_launcher_small)
                .setTicker(this.getString(R.string.scanner_notification_ticker))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(this.getString(R.string.scanner_notification_message)))
                .setContentText(this.getString(R.string.scanner_notification_message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntent);

        startForeground(1, notification.build());
    }

    private void exportRuuvitags() {
        ArrayList<Ruuvitag> templist = new ArrayList<>();
        RuuvitagComplexList ruuvilist = new RuuvitagComplexList();
        for(Ruuvitag ruuvitag : ruuvitagArrayList) {
            if(!ruuvitag.favorite)
                templist.add(ruuvitag);
        }
        ruuvilist.setRuuvitags(templist);
        ComplexPreferences complexPreferences = ComplexPreferences
                .getComplexPreferences(this, "saved_tags", MODE_PRIVATE);
        complexPreferences.putObject("ruuvi", ruuvilist);
        complexPreferences.commit();
    }

    private boolean checkForSameTag(Ruuvitag ruuvi) {
        for(Ruuvitag ruuvitag : ruuvitagArrayList) {
            if(ruuvi.getId().equals(ruuvitag.getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDestroy() {
        exportDB();
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region("uusi", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        beaconManager.unbind(this);
        settings.unregisterOnSharedPreferenceChangeListener(mListener);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Foreground.Listener listener = new Foreground.Listener(){
        public void onBecameForeground(){
            Log.d("tagi", "huuuu");
            if(!isRunning(ScannerService.class))
                startService(new Intent(ScannerService.this, ScannerService.class));
        }
        public void onBecameBackground(){
            if(!settings.getBoolean("pref_bgscan", false))
                stopSelf();
        }
    };

    public void save(Ruuvitag ruuvitag) {
        String time = new SimpleDateFormat("dd-MM-yyyy, hh:mm:ss").format(new Date());

        if(!Exists(ruuvitag.getId())) {
            ContentValues values = new ContentValues();
            values.put(DBContract.RuuvitagDB.COLUMN_ID, ruuvitag.getId());
            values.put(DBContract.RuuvitagDB.COLUMN_URL, ruuvitag.getUrl());
            values.put(DBContract.RuuvitagDB.COLUMN_RSSI, ruuvitag.getRssi());
            values.put(DBContract.RuuvitagDB.COLUMN_TEMP, ruuvitag.getTemperature());
            values.put(DBContract.RuuvitagDB.COLUMN_HUMI, ruuvitag.getHumidity());
            values.put(DBContract.RuuvitagDB.COLUMN_PRES, ruuvitag.getPressure());
            values.put(DBContract.RuuvitagDB.COLUMN_LAST, time);

            long newRowId = db.insert(DBContract.RuuvitagDB.TABLE_NAME, null, values);
        }
    }

    public void update(Ruuvitag ruuvitag) {
        //String time = new SimpleDateFormat("dd-MM-yyyy, hh:mm:ss").format(new Date());
        String time = DateFormat.getDateTimeInstance().format(new Date());

        if(Exists(ruuvitag.getId())) {
            ContentValues values = new ContentValues();
            values.put(DBContract.RuuvitagDB.COLUMN_ID, ruuvitag.getId());
            values.put(DBContract.RuuvitagDB.COLUMN_URL, ruuvitag.getUrl());
            values.put(DBContract.RuuvitagDB.COLUMN_RSSI, ruuvitag.getRssi());
            values.put(DBContract.RuuvitagDB.COLUMN_TEMP, ruuvitag.getTemperature());
            values.put(DBContract.RuuvitagDB.COLUMN_HUMI, ruuvitag.getHumidity());
            values.put(DBContract.RuuvitagDB.COLUMN_PRES, ruuvitag.getPressure());
            values.put(DBContract.RuuvitagDB.COLUMN_LAST, time);

            db.update(DBContract.RuuvitagDB.TABLE_NAME, values, "id="+ DatabaseUtils.sqlEscapeString(ruuvitag.getId()), null);
        }
        exportDB();
    }

    public boolean Exists(String id) {
        cursor = db.rawQuery("select 1 from ruuvitag where "+DBContract.RuuvitagDB.COLUMN_ID+"=?",
                new String[] { id });
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        stopSelf();
        Foreground.get().removeListener(listener);
        handler.close();
        db.close();
    }

    private void exportDB() {
        /*File dbFile = getDatabasePath("ruuvi_db.db");
        File exportDir = new File(Environment.getExternalStorageDirectory(), "ruuvitaglogs");
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e("ScannerService", "failed to create directory");
            }
        }

        try {
            Cursor curCSV = db.rawQuery("SELECT * FROM ruuvitag", null);

            File file;

            String[] columnNames = {
                    curCSV.getColumnName(1),
                    curCSV.getColumnName(7),
                    curCSV.getColumnName(3),
                    curCSV.getColumnName(4),
                    curCSV.getColumnName(5),
                    curCSV.getColumnName(6),
                    curCSV.getColumnName(8)
            };

            String[] arrStr = {
                    curCSV.getString(1),
                    curCSV.getString(7),
                    curCSV.getString(3),
                    curCSV.getString(4),
                    curCSV.getString(5),
                    curCSV.getString(6),
                    curCSV.getString(8)
            };

            while (curCSV.moveToNext()) {
                //Which column you want to export
                file = new File(exportDir, curCSV.getString(1) + "1.0");
                file.createNewFile();
                CSVWriter writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().toString()+"/ruuviaglogs/ruuvi.csv"), ',',
                        CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, "\r\n");
                writer.writeNext(arrStr);
                writer.close();
            }
            curCSV.close();
        } catch (Exception sqlEx) {
            Log.e("ScannerService", sqlEx.getMessage(), sqlEx);
        }*/
        //TODO Logging to csv file
    }

    private boolean isRunning(Class<?> serviceClass) {
        ActivityManager mgr = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : mgr.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}