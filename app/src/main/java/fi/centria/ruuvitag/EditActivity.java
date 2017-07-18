package fi.centria.ruuvitag;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import fi.centria.ruuvitag.database.DBContract;
import fi.centria.ruuvitag.database.DBHandler;

public class EditActivity extends AppCompatActivity {
    private Cursor cursor;
    SQLiteDatabase db;
    DBHandler handler;
    private int index;
    EditText textfield;
    String id;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        textfield = (EditText) findViewById(R.id.input_name);
        handler = new DBHandler(this);
        db = handler.getWritableDatabase();
        index = getIntent().getExtras().getInt("index");
        getData();

        if(name != null && !name.isEmpty())
            textfield.setText(name, TextView.BufferType.NORMAL);
        else
            textfield.setText(id, TextView.BufferType.NORMAL);
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
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    private void getData()
    {
        cursor = db.query(DBContract.RuuvitagDB.TABLE_NAME, null, "_ID= ?", new String[] { "" + index }, null, null, null);
        if(cursor != null)
            cursor.moveToFirst();

        id = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_ID));
        name = cursor.getString(cursor.getColumnIndex(DBContract.RuuvitagDB.COLUMN_NAME));
    }

    public void save(View view) {
        ContentValues values = new ContentValues();
        values.put(DBContract.RuuvitagDB.COLUMN_NAME, textfield.getText().toString());
        db.update(DBContract.RuuvitagDB.TABLE_NAME, values, "_ID="+ index, null);
        cursor.close();
        finish();
    }

    public void delete(View view) {
        final String selection = DBContract.RuuvitagDB._ID + " LIKE ?";
        final String[] selectionArgs = { String.valueOf(index) };

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Ruuvitag");
        builder.setMessage("Are you sure you want to delete this ruuvitag?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                db.delete(DBContract.RuuvitagDB.TABLE_NAME, selection, selectionArgs);
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
    }


}
