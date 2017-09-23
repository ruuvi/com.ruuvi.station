package com.ruuvi.tag.feature.edit;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.tag.R;
import com.ruuvi.tag.adapters.EditAdapter;
import com.ruuvi.tag.model.Alarm;
import com.ruuvi.tag.model.Alarm_Table;
import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.model.TagSensorReading;
import com.ruuvi.tag.model.TagSensorReading_Table;

public class EditActivity extends AppCompatActivity {
    EditText nameInput;
    EditText gatewayInput;
    RuuviTag tag;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_edit);

        nameInput = findViewById(R.id.input_name);
        gatewayInput = findViewById(R.id.input_gatewayUrl);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_alarm);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAlarmEdit(view);
            }
        });

        if(getIntent().getExtras() != null) {
            tag = RuuviTag.get(getIntent().getExtras().getString("id"));
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.tag_not_found), Toast.LENGTH_SHORT).show();
            finish();
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
            save(null);
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
        tag = RuuviTag.get(tag.id);

        List<Alarm> alarms = Alarm.getForTag(tag.id);
        EditAdapter adapter = new EditAdapter(this, alarms);
        ListView listView = (ListView) findViewById(R.id.alarmlist);
        listView.setAdapter(adapter);

        if(tag.name != null && !tag.name.isEmpty())
            nameInput.setText(tag.name, TextView.BufferType.NORMAL);

        if(tag.gatewayUrl != null && !tag.gatewayUrl.isEmpty())
            gatewayInput.setText(tag.gatewayUrl, TextView.BufferType.NORMAL);
    }

    public void save(View view) {
        tag = RuuviTag.get(tag.id);
        tag.name = nameInput.getText().toString();
        tag.gatewayUrl = gatewayInput.getText().toString();
        tag.update();
    }

    public void delete(View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.tag_delete_title));
        builder.setMessage(getString(R.string.tag_delete_message));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                tag.deleteTagAndRelatives();
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }

    public void openAlarmEdit(View view) {
        Intent intent = new Intent(EditActivity.this, AlarmEditActivity.class);
        intent.putExtra("tagId", tag.id);
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
}
