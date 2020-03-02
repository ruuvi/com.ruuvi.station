package com.ruuvi.station.feature.main;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.ruuvi.station.R;
import com.ruuvi.station.bluetooth.FoundRuuviTag;
import com.ruuvi.station.bluetooth.IRuuviTagScanner;
import com.ruuvi.station.database.RuuviTagRepository;
import com.ruuvi.station.feature.AboutActivity;
import com.ruuvi.station.feature.AddTagActivity;
import com.ruuvi.station.feature.AppSettingsActivity;
import com.ruuvi.station.feature.WelcomeActivity;
import com.ruuvi.station.model.RuuviTagEntity;
import com.ruuvi.station.util.DataUpdateListener;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.Utils;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IRuuviTagScanner.OnTagFoundListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1337;
    private static final int TAG_UI_UPDATE_FREQ = 1000;
    private static final int FROM_WELCOME = 1447;
    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    private DrawerLayout drawerLayout;
    public List<RuuviTagEntity> myRuuviTags = new ArrayList<>();
    public List<RuuviTagEntity> otherRuuviTags = new ArrayList<>();
    private DataUpdateListener fragmentWithCallback;
    private Handler handler;
    private boolean dashboardVisible = true;
    private Preferences prefs;

    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            if (fragmentWithCallback != null) fragmentWithCallback.dataUpdated();
            handler.postDelayed(updater, TAG_UI_UPDATE_FREQ);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main_drawerLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.logo);

        handler = new Handler();
        prefs = new Preferences(this);
        myRuuviTags = new ArrayList<>(RuuviTagRepository.getAll(true));

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );

        drawerLayout.addDrawerListener(drawerToggle);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
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
        if (prefs.isFirstStart()) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivityForResult(intent, FROM_WELCOME);
        } else {
            getThingsStarted(false);
        }
    }

    public static boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    AdapterView.OnItemClickListener drawerItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // TODO: 10/10/17 make this in a sane way
            openFragment(i);
        }
    };

    public static void setBackgroundScanning(final Context context) {
        Log.d(TAG, "DEBUG, setBackgroundScan");
    }

    public static void requestIgnoreBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = context.getPackageName();
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(packageName))
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                else {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                }
                context.startActivity(intent);
            } catch (Exception e) {
                Log.d(TAG, "Could not set ignoring battery optimization");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // party
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        requestPermissions();
                    } else {
                        showPermissionSnackbar(this);
                    }
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showPermissionSnackbar(final Activity activity) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main_contentFrame), getString(R.string.location_permission_needed), Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.settings), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        });
        snackbar.show();
    }

    private List<String> getNeededPermissions() {
        int permissionCoarseLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        final List<String> listPermissionsNeeded = new ArrayList<>();

        if(permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        int permissionFineLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        return listPermissionsNeeded;
    }

    private boolean showPermissionDialog(AppCompatActivity activity) {
        List<String> listPermissionsNeeded = getNeededPermissions();

        if(!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_CODE_LOCATION_PERMISSIONS
            );
        }

        return !listPermissionsNeeded.isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getNeededPermissions().size() > 0) {

        } else {
            refreshTagLists();
            handler.post(updater);
        }
    }

    private void refreshTagLists() {
        myRuuviTags.clear();
        myRuuviTags.addAll(RuuviTagRepository.getAll(true));
        otherRuuviTags.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updater);
        for (RuuviTagEntity tag : myRuuviTags) {
            RuuviTagRepository.update(tag);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void openFragment(int type) {
        Fragment fragment = null;
        dashboardVisible = false;
        switch (type) {
            case 1:
                refreshTagLists();
                fragment = new DashboardFragment();
                fragmentWithCallback = (DataUpdateListener)fragment;
                dashboardVisible = true;
                break;
            case 2:
                Intent settingsIntent = new Intent(this, AppSettingsActivity.class);
                startActivity(settingsIntent);
                return;
            case 3:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return;
            default:
                refreshTagLists();
                Intent addIntent = new Intent(this, AddTagActivity.class);
                startActivity(addIntent);
                type = 0;
                return;
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.main_contentFrame, fragment)
                .commit();
        if ((type == 1 || type == 3) && getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.drawable.logo);
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(null);
        }
        setTitle(getResources().getStringArray(R.array.navigation_items_titles)[type]);
        drawerLayout.closeDrawers();
    }

    @Override
    public void onTagFound(FoundRuuviTag foundTag) {
        final RuuviTagEntity tag = new RuuviTagEntity(foundTag);
        for (int i = 0; i < myRuuviTags.size(); i++) {
            final String tagId = myRuuviTags.get(i).getId();

            if (tagId != null && tagId.equals(tag.getId())) {
                myRuuviTags.set(i, tag);
                if (fragmentWithCallback != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fragmentWithCallback.dataUpdated();
                        }
                    });
                }
                return;
            }
        }

        for (int i = 0; i < otherRuuviTags.size(); i++) {
            String tagId = otherRuuviTags.get(i).getId();
            if (tagId != null && tagId.equals(tag.getId())) {
                otherRuuviTags.set(i, tag);
                Utils.sortTagsByRssi(otherRuuviTags);
                if (fragmentWithCallback != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fragmentWithCallback.dataUpdated();
                        }
                    });
                }
                return;
            }
        }
        otherRuuviTags.add(tag);
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, "Could not get LOCATION_MODE");
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ENABLE_BT) {

            }
        } else {
            if (requestCode == FROM_WELCOME) {
                getThingsStarted(true);
            }
        }
    }

    private void getThingsStarted(boolean goToAddTags) {
        if (isBluetoothEnabled()) {

        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        prefs.setFirstStart(false);
        openFragment(goToAddTags ? 0 : 1);
        requestPermissions();
    }

    private void requestPermissions() {
        if (getNeededPermissions().size() > 0) {
            final AppCompatActivity activity = this;
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.permission_dialog_title));
            alertDialog.setMessage(getString(R.string.permission_dialog_request_message));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    showPermissionDialog(activity);
                }
            });
            alertDialog.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (dashboardVisible) {
            super.onBackPressed();
        } else {
            openFragment(1);
        }
    }
}

