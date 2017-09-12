package com.ruuvi.tag.feature.edit;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

import com.ruuvi.tag.R;
import com.ruuvi.tag.model.Alarm;

public class EditActivity extends AppCompatActivity {
    private int index;
    EditText textfield;
    String id;
    String name;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_edit);
        textfield = (EditText) findViewById(R.id.input_name);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAlarmEdit(view);
                save(null);
            }
        });

        if(getIntent().getExtras() != null) {
            index = getIntent().getExtras().getInt("index");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            this.save(null);
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

    private void getData() {
        // TODO: 12/09/17 load tag data
        /*
        cursor = db.query(DBContract.RuuviTagDB.TABLE_NAME, null, "_ID= ?", new String[] { "" + index }, null, null, null);

        if(cursor != null)
            cursor.moveToFirst();

        id = cursor.getString(cursor.getColumnIndex(DBContract.RuuviTagDB.COLUMN_ID));
        name = cursor.getString(cursor.getColumnIndex(DBContract.RuuviTagDB.COLUMN_NAME));
        String alarms = cursor.getString(cursor.getColumnIndex(DBContract.RuuviTagDB.COLUMN_VALUES));

        ArrayList<Alarm> alarmValues = readSeparated(alarms);
        EditAdapter adapter = new EditAdapter(this, alarmValues);
        ListView listView = (ListView) findViewById(R.id.alarmlist);
        listView.setAdapter(adapter);

        if(name != null && !name.isEmpty())
            textfield.setText(name, TextView.BufferType.NORMAL);
        else
            textfield.setText(id, TextView.BufferType.NORMAL);
        */
    }

    public void save(View view) {
        // TODO: 12/09/17 update tag
        /*
        ContentValues values = new ContentValues();
        values.put(DBContract.RuuviTagDB.COLUMN_NAME, textfield.getText().toString());
        db.update(DBContract.RuuviTagDB.TABLE_NAME, values, "_ID="+ index, null);
        cursor.close();
        */
    }

    public void delete(View view) {
        // TODO: 12/09/17 delete tag
        /*
        final String selection = DBContract.RuuviTagDB._ID + " LIKE ?";
        final String[] selectionArgs = { String.valueOf(index) };

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete RuuviTag");
        builder.setMessage("Are you sure you want to delete this ruuvitag?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                db.delete(DBContract.RuuviTagDB.TABLE_NAME, selection, selectionArgs);
                cursor.close();
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
        */
    }

    public void openAlarmEdit(View view) {
        Intent intent = new Intent(EditActivity.this, AlarmEditActivity.class);
        intent.putExtra("index", index);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public ArrayList<Alarm> readSeparated(String data) {
        String[] linevector;
        int index = 0;

        linevector = data.split(",");

        Integer[] values = new Integer[8];

        for(String l : linevector) {
            try {
                values[index] = (Integer.parseInt(l));
            } catch (NumberFormatException e) {
                values[index] = (null);
            }
            index++;
        }

        ArrayList<Alarm> alarms = new ArrayList<>();

        if(values[0] != -500 && values[1] != -500) {
            alarms.add(new Alarm(values[0], values[1], "Temperature"));
        }
        if(values[2] != -500 && values[3] != -500) {
            alarms.add(new Alarm(values[2], values[3], "Humidity"));
        }
        if(values[4] != -500 && values[5] != -500) {
            alarms.add(new Alarm(values[4], values[5], "Pressure"));
        }
        if(values[6] != -500 && values[7] != -500) {
            alarms.add(new Alarm(values[6], values[7], "RSSI"));
        }

        return alarms;
    }
}
