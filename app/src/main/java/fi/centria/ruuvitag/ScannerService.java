package fi.centria.ruuvitag;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.neovisionaries.bluetooth.ble.advertising.ADManufacturerSpecific;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneUID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.opencsv.CSVWriter;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fi.centria.ruuvitag.database.DBContract;
import fi.centria.ruuvitag.database.DBHandler;
import fi.centria.ruuvitag.model.ScanEvent;
import fi.centria.ruuvitag.util.ComplexPreferences;
import fi.centria.ruuvitag.util.DeviceIdentifier;
import fi.centria.ruuvitag.util.Foreground;
import fi.centria.ruuvitag.util.PlotSource;
import fi.centria.ruuvitag.util.Ruuvitag;
import fi.centria.ruuvitag.util.RuuvitagComplexList;



public class ScannerService extends Service /*implements BeaconConsumer*/
{
    private ArrayList<LeScanResult> scanResults;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService alertScheduler;

    private class LeScanResult
    {
        BluetoothDevice device;
        int rssi;
        byte[] scanData;
    }

    List<Ruuvitag> ruuvitagArrayList;
    //private BeaconManager beaconManager;
    private BackgroundPowerSaver bps;
    SharedPreferences settings;
    DBHandler handler;
    SQLiteDatabase db;
    Cursor cursor;
    Region region;
    private String[] titles;
    private String backendUrl;
    private PlotSource plotSource;
    private Integer[] alertValues;
    private int notificationId;
    private int MAX_NUM_NOTIFICATIONS = 5;
    private Timer timer;
    private NotificationCompat.Builder notification;


    private BluetoothAdapter bluetoothAdapter;
    private Handler scanTimerHandler;
    private static int MAX_SCAN_TIME_MS = 1000;
    private boolean scanning;

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
        notificationId = 1512;

        titles = new String[]{ getString(R.string.alert_notification_title0),
                getString(R.string.alert_notification_title1),
                getString(R.string.alert_notification_title2),
                getString(R.string.alert_notification_title3),
                getString(R.string.alert_notification_title4),
                getString(R.string.alert_notification_title5),
                getString(R.string.alert_notification_title6),
                getString(R.string.alert_notification_title7)
        };

        if(settings.getBoolean("pref_bgscan", false))
             startFG();

        Foreground.init(getApplication());
        Foreground.get().addListener(listener);

        bps = new BackgroundPowerSaver(this);

        ruuvitagArrayList = new ArrayList<>();

        handler = new DBHandler(getApplicationContext());
        db = handler.getWritableDatabase();

/*
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // Detect the URL frame:
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));

        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.))

        beaconManager.bind(this);*/

        backendUrl = settings.getString("pref_backend", null);

        plotSource =  PlotSource.getInstance();

        scanTimerHandler = new Handler();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        scheduler = Executors.newSingleThreadScheduledExecutor();
        int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "5")) * 1000;
        scheduler.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                if(!scheduler.isShutdown())
                 startScan();
            }
        }, 0, scanInterval-MAX_SCAN_TIME_MS+1, TimeUnit.MILLISECONDS);

        timer = new Timer();
        TimerTask alertManager = new ScannerService.alertManager();
        timer.scheduleAtFixedRate(alertManager, 2500, 2500);
    }

    private void startScan() {
        if(scanning)
            return;

            scanTimerHandler.postDelayed(new Runnable() {
                @Override
                public void run()
                {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                    processFoundDevices();
                }
            }, MAX_SCAN_TIME_MS);

        scanResults = new ArrayList<LeScanResult>();
        scanning = true;
        bluetoothAdapter.startLeScan(mLeScanCallback);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    Iterator<LeScanResult> itr = scanResults.iterator();

                    LeScanResult dev = new LeScanResult();
                    dev.device = device;
                    dev.rssi = rssi;
                    dev.scanData = scanRecord;

                    boolean devFound = false;

                    while(itr.hasNext())
                    {
                        LeScanResult element = itr.next();
                        if(device.getAddress().equalsIgnoreCase(element.device.getAddress()))
                            devFound = true;
                    }


                    if(!devFound)
                    {
                        scanResults.add(dev);
                    }
                }
            };


    void processFoundDevices()
    {
        ruuvitagArrayList.clear();
        ScanEvent scanEvent = new ScanEvent(getApplicationContext(), DeviceIdentifier.id(getApplicationContext()));

        Iterator<LeScanResult> itr = scanResults.iterator();
        while(itr.hasNext())
        {
            LeScanResult element = itr.next();

            // Parse the payload of the advertisement packet
            // as a list of AD structures.
            List<ADStructure> structures =
                    ADPayloadParser.getInstance().parse(element.scanData);

            // For each AD structure contained in the advertisement packet.
            for (ADStructure structure : structures)
            {
                if (structure instanceof EddystoneURL)
                {
                    // Eddystone URL
                    EddystoneURL es = (EddystoneURL)structure;
                    if (es.getURL().toString().startsWith("https://ruu.vi/#") || es.getURL().toString().startsWith("https://r/"))
                    {
                        // Creates temporary ruuvitag-object, without heavy calculations
                        Ruuvitag temp = new Ruuvitag(element.device.getAddress(),es.getURL().toString(),null,""+element.rssi, true);
                        if(checkForSameTag(temp)) {
                            // Creates real object, with temperature etc. calculated
                            Ruuvitag real = new Ruuvitag(element.device.getAddress(),es.getURL().toString(),null,""+element.rssi, false);
                            ruuvitagArrayList.add(real);
                            update(real);
                            scanEvent.addRuuvitag(real);
                        }
                    }

                }
                // If the AD structure represents Eddystone TLM.
                else if(structure instanceof ADManufacturerSpecific)
                {
                    ADManufacturerSpecific es = (ADManufacturerSpecific)structure;
                    if(es.getCompanyId() == 0x0499)
                    {

                        byte[] data = es.getData();
                        if (data != null) {

                            Ruuvitag tempTag = new Ruuvitag(element.device.getAddress(),null,data,""+element.rssi, true);
                            if(checkForSameTag(tempTag)) {
                                // Creates real object, with temperature etc. calculated
                                Ruuvitag real = new Ruuvitag(element.device.getAddress(),null,data,""+element.rssi, false);
                                ruuvitagArrayList.add(real);
                                update(real);
                                scanEvent.addRuuvitag(real);
                            }
                        }
                    }


                }
            }

/*
            if(beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                // Parse url from beacon advert
                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());

            }*/

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
    }

    public SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if(settings.getBoolean("pref_bgscan", false))
                startFG();

            if(!settings.getBoolean("pref_bgscan", false))
                stopForeground(true);

            backendUrl = settings.getString("pref_backend",null);

            scheduler.shutdown();

            scheduler = Executors.newSingleThreadScheduledExecutor();
            int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "5")) * 1000;
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run()
                {
                    if(!scheduler.isShutdown())
                        startScan();
                }
            }, 0, scanInterval-MAX_SCAN_TIME_MS+1, TimeUnit.MILLISECONDS);

            /*
            try
            {
                beaconManager.setForegroundBetweenScanPeriod
                        (Long.parseLong(settings.getString("pref_scaninterval", "1")) * 1000 - 1000l);
                beaconManager.updateScanPeriods();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            }*/
        }
    };

    /*
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {
                ruuvitagArrayList.clear();

                ScanEvent scanEvent = new ScanEvent(getApplicationContext(), DeviceIdentifier.id(getApplicationContext()));

                for(Beacon beacon : beacons) {
                    if(beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x10) {
                        // Parse url from beacon advert
                        String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                        if (url.startsWith("https://ruu.vi/#") || url.startsWith("https://r/")) {
                            // Creates temporary ruuvitag-object, without heavy calculations
                            Ruuvitag temp = new Ruuvitag(beacon, true);
                            if(checkForSameTag(temp)) {
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
    */

    public void startFG()
    {
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

        startForeground(notificationId, notification.build());
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
    public void onDestroy()
    {
        exportDB();

        scheduler.shutdown();
        /*
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region("uusi", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        beaconManager.unbind(this);*/

        settings.unregisterOnSharedPreferenceChangeListener(mListener);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Foreground.Listener listener = new Foreground.Listener()
    {
        public void onBecameForeground()
        {
            if(!isRunning(ScannerService.class))
                startService(new Intent(ScannerService.this, ScannerService.class));
        }
        public void onBecameBackground()
        {
            if(!settings.getBoolean("pref_bgscan", false))
            {
                timer.cancel();
                scheduler.shutdown();
                stopSelf();
            }
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
            values.put(DBContract.RuuvitagDB.COLUMN_VALUES, "-500,-500,-500,-500,-500,-500,-500,-500");

            long newRowId = db.insert(DBContract.RuuvitagDB.TABLE_NAME, null, values);
        }
    }

    public void update(Ruuvitag ruuvitag) {
        String time = new SimpleDateFormat("dd-MM-yyyy, hh:mm:ss").format(new Date());

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
            exportDB();
        }
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
        File exportDir = new File(Environment.getExternalStorageDirectory(), "ruuvitaglogs");
        String time = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e("ScannerService", "failed to create directory");
            }
        }

        try {
            Cursor curCSV = db.rawQuery("SELECT * FROM ruuvitag", null);

            String[] columnNames = {
                    curCSV.getColumnName(1),
                    curCSV.getColumnName(7),
                    curCSV.getColumnName(3),
                    curCSV.getColumnName(4),
                    curCSV.getColumnName(5),
                    curCSV.getColumnName(6),
                    curCSV.getColumnName(8)
            };

            while (curCSV.moveToNext()) {
                File file = new File(exportDir, curCSV.getString(1)+"-"+time+".csv");
                FileWriter fw = new FileWriter(file, file.exists());

                CSVWriter writer = new CSVWriter(fw);

                if(file.length() <= 0) {
                    writer.writeNext(columnNames);
                }

                String[] arrStr = {
                        curCSV.getString(1),
                        curCSV.getString(7),
                        curCSV.getString(3),
                        curCSV.getString(4),
                        curCSV.getString(5),
                        curCSV.getString(6),
                        curCSV.getString(9).substring(12, 20)
                };

                writer.writeNext(arrStr);
                writer.close();
                fw.close();
            }
            curCSV.close();

        } catch (Exception sqlEx) {
            Log.e("ScannerService", sqlEx.getMessage(), sqlEx);
        }
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

    private class alertManager extends TimerTask {
        private Handler handler = new Handler();

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Cursor csr = db.rawQuery("SELECT * FROM " + DBContract.RuuvitagDB.TABLE_NAME, null);
                            while (csr.moveToNext()) {
                                String _id = csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB._ID));
                                String id = csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_ID));
                                String name = csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_NAME));
                                Double temp = Double.parseDouble(csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_TEMP)));
                                Double humi = Double.parseDouble(csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_HUMI)));
                                Double pres = Double.parseDouble(csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_PRES)));
                                Double rssi = Double.parseDouble(csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_RSSI)));
                                alertValues = readSeparated(csr.getString(csr.getColumnIndex(DBContract.RuuvitagDB.COLUMN_VALUES)));

                                if(name == null)
                                    name = id;

                                if (alertValues[0] != -500 && temp < alertValues[0]) {
                                    sendAlert(0, _id, name);
                                }
                                if (alertValues[1] != -500 && temp > alertValues[1]) {
                                    sendAlert(1, _id, name);
                                }
                                if (alertValues[2] != -500 && humi < alertValues[2]) {
                                    sendAlert(2, _id, name);
                                }
                                if (alertValues[3] != -500 && humi > alertValues[3]) {
                                    sendAlert(3, _id, name);
                                }
                                if (alertValues[4] != -500 && pres < alertValues[4]) {
                                    sendAlert(4, _id, name);
                                }
                                if (alertValues[5] != -500 && pres > alertValues[5]) {
                                    sendAlert(5, _id, name);
                                }
                                if (alertValues[6] != -500 && rssi < alertValues[6]) {
                                    sendAlert(6, _id, name);
                                }
                                if (alertValues[7] != -500 && rssi > alertValues[7]) {
                                    sendAlert(7, _id, name);
                                }
                            }
                            csr.close();
                        }
                    });
                }
            }).start();
        }
    }

    private void sendAlert(int type, String _id, String name) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        int notificationid = Integer.parseInt(_id + String.valueOf(type));

        if(notification == null) {
            notification
                    = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(name)
                    .setSmallIcon(R.mipmap.ic_launcher_small)
                    .setTicker(name + " " + titles[type])
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(titles[type]))
                    .setContentText(titles[type])
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setLargeIcon(bitmap);
        } else {
            notification.setContentTitle(name)
                    .setContentText(titles[type]);
        }

        NotificationManager NotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotifyMgr.notify(notificationid, notification.build());
    }

    public Integer[] readSeparated(String data) {
        String[] linevector;
        int index = 0;

        linevector = data.split(",");

        Integer[] values = new Integer[linevector.length];

        for(String l : linevector) {
            try {
                values[index] = Integer.parseInt(l);
            } catch (NumberFormatException e) {
                values[index] = null;
            }
            index++;
        }

        return values;
    }
}