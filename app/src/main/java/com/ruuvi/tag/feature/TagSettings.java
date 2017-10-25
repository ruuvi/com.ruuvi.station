package com.ruuvi.tag.feature;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.CharacterPickerDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ruuvi.tag.R;
import com.ruuvi.tag.model.RuuviTag;

public class TagSettings extends AppCompatActivity {
    public static final String TAG_ID = "TAG_ID";

    private RuuviTag tag;

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

        final TextView nameTextView = findViewById(R.id.input_name);
        nameTextView.setText(!tag.name.isEmpty() ? tag.name : tag.id);

        final TextView gatewayTextView = findViewById(R.id.input_gatewayUrl);
        if (!tag.gatewayUrl.isEmpty()) gatewayTextView.setText(tag.gatewayUrl);

        // TODO: 25/10/17 make this less ugly
        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TagSettings.this, R.style.Theme_AppCompat_Light_Dialog);
                builder.setTitle(getString(R.string.tag_name));
                final EditText input = new EditText(TagSettings.this);
                input.setText(tag.name);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tag.name = input.getText().toString();
                        nameTextView.setText(tag.name);
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });

        gatewayTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TagSettings.this, R.style.Theme_AppCompat_Light_Dialog);
                builder.setTitle(getString(R.string.gateway_url));
                final EditText input = new EditText(TagSettings.this);
                input.setText(tag.gatewayUrl);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tag.gatewayUrl = input.getText().toString();
                        gatewayTextView.setText(!tag.gatewayUrl.isEmpty() ? tag.gatewayUrl : getString(R.string.no_gateway_url));
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            tag.update();
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
}
