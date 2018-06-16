package com.ruuvi.station.feature;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.BottomSheetDialog;
import android.support.media.ExifInterface;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.ruuvi.station.R;
import com.ruuvi.station.model.Alarm;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TagSettings extends AppCompatActivity {
    private static final String TAG = "TagSettings";
    public static final String TAG_ID = "TAG_ID";

    private RuuviTag tag;
    List<Alarm> tagAlarms = new ArrayList<>();
    List<AlarmItem> alarmItems = new ArrayList<>();
    private boolean somethinghaschanged = false;
    private Uri file;
    AppCompatImageView tagImage;

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

        ((TextView)findViewById(R.id.input_mac)).setText(tag.id);
        findViewById(R.id.input_mac).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Mac address", tag.id);
                try {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(TagSettings.this, "Mac address copied to clipboard", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.d(TAG, "Could not copy mac to clipboard");
                }
                return false;
            }
        });

        tagImage = findViewById(R.id.tag_image);
        tagImage.setImageBitmap(Utils.getBackground(this, tag));
        findViewById(R.id.tag_image_camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageSourceSheet();
            }
        });

        findViewById(R.id.tag_image_select_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tag.defaultBackground = tag.defaultBackground == 8 ? 0 : tag.defaultBackground + 1;
                tag.userBackground = null;
                tagImage.setImageDrawable(Utils.getDefaultBackground(tag.defaultBackground, getApplicationContext()));
                somethinghaschanged = true;
            }
        });

        final TextView nameTextView = findViewById(R.id.input_name);
        nameTextView.setText(tag.getDispayName());

        final TextView gatewayTextView = findViewById(R.id.input_gatewayUrl);
        if (tag.gatewayUrl != null && !tag.gatewayUrl.isEmpty()) gatewayTextView.setText(tag.gatewayUrl);

        // TODO: 25/10/17 make this less ugly
        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(TagSettings.this, R.style.AppTheme));
                builder.setTitle(getString(R.string.tag_name));
                final EditText input = new EditText(TagSettings.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(tag.name);
                FrameLayout container = new FrameLayout(getApplicationContext());
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                input.setLayoutParams(params);
                container.addView(input);
                builder.setView(container);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        somethinghaschanged = true;
                        tag.name = input.getText().toString();
                        nameTextView.setText(tag.name);
                    }
                });
                builder.setNegativeButton("Cancel", null);
                Dialog d = builder.create();
                try {
                    d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } catch (Exception e) {
                    Log.d(TAG, "Could not open keyboard");
                }
                d.show();
                input.requestFocus();
            }
        });

        gatewayTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(TagSettings.this, R.style.AppTheme));
                builder.setTitle(getString(R.string.gateway_url));
                final EditText input = new EditText(TagSettings.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(tag.gatewayUrl);
                FrameLayout container = new FrameLayout(getApplicationContext());
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                input.setLayoutParams(params);
                container.addView(input);
                builder.setView(container);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        somethinghaschanged = true;
                        tag.gatewayUrl = input.getText().toString();
                        gatewayTextView.setText(!tag.gatewayUrl.isEmpty() ? tag.gatewayUrl : getString(R.string.no_gateway_url));
                    }
                });
                builder.setNegativeButton("Cancel", null);
                Dialog d = builder.create();
                try {
                    d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } catch (Exception e) {
                    Log.d(TAG, "Could not open keyboard");
                }
                d.show();
                input.requestFocus();
            }
        });

        alarmItems.add(new AlarmItem(getString(R.string.temperature), Alarm.TEMPERATURE, false, -40, 85));
        alarmItems.add(new AlarmItem(getString(R.string.humidity), Alarm.HUMIDITY, false, 0, 100));
        alarmItems.add(new AlarmItem(getString(R.string.pressure), Alarm.PERSSURE, false, 300, 1100));
        alarmItems.add(new AlarmItem(getString(R.string.rssi), Alarm.RSSI, false, -105 ,0));
        alarmItems.add(new AlarmItem(getString(R.string.movement), Alarm.MOVEMENT, false, 0 ,0));

        for (Alarm alarm: tagAlarms) {
            AlarmItem item = alarmItems.get(alarm.type);
            item.high = alarm.high;
            item.low = alarm.low;
            item.checked = true;
            item.alarm = alarm;
        }

        LayoutInflater inflater = getLayoutInflater();
        ConstraintLayout parentLayout = findViewById(R.id.alerts_container);

        for (int i = 0; i < alarmItems.size(); i++) {
            AlarmItem item = alarmItems.get(i);
            item.view = inflater.inflate(R.layout.view_alarm, parentLayout, false);
            item.view.setId(View.generateViewId());
            CheckBox checkBox = item.view.findViewById(R.id.alert_checkbox);
            checkBox.setTag(i);
            checkBox.setOnCheckedChangeListener(alarmCheckboxListener);
            item.createView();
            parentLayout.addView(item.view);
            ConstraintSet set = new ConstraintSet();
            set.clone(parentLayout);
            set.connect(item.view.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            set.connect(item.view.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            set.connect(item.view.getId(), ConstraintSet.TOP, (i == 0 ? ConstraintSet.PARENT_ID : alarmItems.get(i - 1).view.getId()), ConstraintSet.BOTTOM);
            set.applyTo(parentLayout);
        }
    }

    CompoundButton.OnCheckedChangeListener alarmCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            somethinghaschanged = true;
            AlarmItem ai = alarmItems.get((int)buttonView.getTag());
            ai.checked = isChecked;
            ai.updateView();
        }
    };

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
        public Alarm alarm;

        public AlarmItem(String name, int type, boolean checked, int min, int max) {
            this.name = name;
            this.type = type;
            this.checked = checked;
            this.min = min;
            this.max = max;
            this.low = min;
            this.high = max;
        }

        public void createView() {
            CrystalRangeSeekbar seekBar = this.view.findViewById(R.id.alert_seekBar);
            seekBar.setMinValue(this.min);
            seekBar.setMaxValue(this.max);
            seekBar.setMinStartValue(this.low);
            seekBar.setMaxStartValue(this.high);
            seekBar.apply();

            seekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
                @Override
                public void valueChanged(Number minValue, Number maxValue) {
                    if (low != minValue.intValue() || high != maxValue.intValue()) {
                        somethinghaschanged = true;
                    }
                    low = minValue.intValue();
                    high = maxValue.intValue();
                    updateView();
                }
            });

            if (this.min == 0 && this.max == 0) {
                seekBar.setVisibility(View.GONE);
                this.view.findViewById(R.id.alert_min_value).setVisibility(View.GONE);
                this.view.findViewById(R.id.alert_max_value).setVisibility(View.GONE);
            }

            updateView();
        }

        public void updateView() {
            CrystalRangeSeekbar seekBar = this.view.findViewById(R.id.alert_seekBar);
            int setSeekbarColor = R.color.ap_gray;
            if (this.checked) {
                setSeekbarColor = R.color.main;
                seekBar.setLeftThumbDrawable(R.drawable.range_ball);
                seekBar.setRightThumbDrawable(R.drawable.range_ball);
                this.subtitle = getString(R.string.alert_substring_movement);
                if (this.type == Alarm.MOVEMENT) {
                    this.subtitle = getString(R.string.alert_substring_movement);
                } else {
                    this.subtitle = String.format(getString(R.string.alert_subtitle_on), this.low, this.high);
                }
            } else {
                seekBar.setLeftThumbDrawable(R.drawable.range_ball_inactive);
                seekBar.setRightThumbDrawable(R.drawable.range_ball_inactive);
                this.subtitle = getString(R.string.alert_subtitle_off);
            }
            seekBar.setBarHighlightColor(getResources().getColor(setSeekbarColor));
            seekBar.setEnabled(this.checked);
            ((CheckBox)this.view.findViewById(R.id.alert_checkbox)).setChecked(this.checked);
            ((TextView)this.view.findViewById(R.id.alert_title)).setText(this.name);
            ((TextView)this.view.findViewById(R.id.alert_subtitle)).setText(this.subtitle);
            ((TextView)this.view.findViewById(R.id.alert_min_value)).setText(this.low + "");
            ((TextView)this.view.findViewById(R.id.alert_max_value)).setText(this.high + "");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tag.favorite = true;
        tag.update();
        for (AlarmItem alarmItem: alarmItems) {
            if (alarmItem.checked) {
                if (alarmItem.alarm == null) {
                    alarmItem.alarm = new Alarm(alarmItem.low, alarmItem.high, alarmItem.type, tag.id);
                    alarmItem.alarm.save();
                } else {
                    alarmItem.alarm.low = alarmItem.low;
                    alarmItem.alarm.high = alarmItem.high;
                    alarmItem.alarm.update();
                }
            } else if (alarmItem.alarm != null) {
                alarmItem.alarm.delete();
            }
        }
        // what are you doing here?
        //finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private void showImageSourceSheet() {
        final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        ListView listView = new ListView(this);
        String[] menu = {
                getResources().getString(R.string.camera),
                getResources().getString(R.string.gallery)
        };

        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menu));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        dispatchTakePictureIntent();
                        break;
                    case 1:
                        getImageFromGallery();
                        break;
                }
                sheetDialog.dismiss();
            }
        });

        sheetDialog.setContentView(listView);
        sheetDialog.show();
    }


    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        String imageFileName = "background_" + tag.id;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_GALLERY_PHOTO = 2;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoFile.createNewFile();
                } catch (IOException ioEx) {
                    Toast.makeText(this, "Could not start camera", Toast.LENGTH_SHORT).show();
                    return;
                }

                file = FileProvider.getUriForFile(this,
                        "com.ruuvi.station.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, file);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public void getImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (file != null) {
                int rotation = getCameraPhotoOrientation(file);
                resize(file, rotation);
                tag.userBackground = file.toString();
                Bitmap background = Utils.getBackground(getApplicationContext(), tag);
                tagImage.setImageBitmap(background);
                somethinghaschanged = true;
            }
        } else if (requestCode == REQUEST_GALLERY_PHOTO && resultCode == RESULT_OK) {
            try {
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // This is fine :)
                }
                if (photoFile != null) {
                    try {
                        photoFile.createNewFile();
                    } catch (IOException ioEx) {
                        // :(
                        return;
                    }
                    OutputStream output = new FileOutputStream(photoFile);
                    try {
                        byte[] buffer = new byte[4 * 1024]; // or other buffer size
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                        output.flush();
                    } finally {
                        output.close();
                    }
                    Uri uri = Uri.fromFile(photoFile);
                    int rotation = getCameraPhotoOrientation(uri);
                    resize(uri, rotation);
                    tag.userBackground = uri.toString();
                    Bitmap background = Utils.getBackground(getApplicationContext(), tag);
                    tagImage.setImageBitmap(background);
                    somethinghaschanged = true;
                }
            } catch (Exception e) {
                // ... O.o
            }
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public int getCameraPhotoOrientation(Uri file){
        int rotate = 0;
        try (InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(file)) {
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get orientation of image");
        }
        return rotate;
    }

    public void resize(Uri uri, int rotation) {
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            if (height > 1440) height = 1440;
            Bitmap b = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), uri);
            Bitmap out = Bitmap.createScaledBitmap(b, height, (int)(((float)height / (float)b.getWidth()) * b.getHeight()), false);
            out = rotate(out, rotation);
            File file = new File(mCurrentPhotoPath);
            FileOutputStream fOut = new FileOutputStream(file);
            out.compress(Bitmap.CompressFormat.JPEG, 60, fOut);
            fOut.flush();
            fOut.close();
            b.recycle();
            out.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Could not resize background image");
        }
    }
}
