package fi.centria.ruuvitag;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

//import tki.centria.ruuviscanner.database.DBContract;
//import tki.centria.ruuviscanner.database.DBHandler;

public class GraphActivity extends AppCompatActivity {
    /*private final Handler handler = new Handler();
    private Runnable timer1;
    private LineGraphSeries<DataPoint> series1;
    private double graph2lastXValue = 5d;
    private SQLiteDatabase db;
    private DBHandler dbhandler;
    private int xindex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        dbhandler = new DBHandler(this);
        db = dbhandler.getReadableDatabase();

        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this, new SimpleDateFormat("HH:mm:ss")));
        graph.getGridLabelRenderer().setHorizontalLabelsAngle(90);
        series1 = new LineGraphSeries<>();
        graph.addSeries(series1);
    }

    @Override
    public void onResume() {
        super.onResume();
        timer1 = new Runnable() {
            @Override
            public void run() {
                graph2lastXValue += 1d;
                series1.appendData(getData(), false, 40);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timer1);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(timer1);
        super.onPause();
    }

    public DataPoint getData() {
        DataPoint v;
        String[] arrStr = new String[]{};
        Cursor curCSV = queryDB();
        Date date = new Date();

        while (curCSV.moveToNext()) {
            arrStr = new String[] {
                    curCSV.getString(0),
                    curCSV.getString(1)
            };
        }

        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        try {
            date = formatter.parse(arrStr[1].substring(11, 19));
            Log.d("tagi", date.toString());
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        curCSV.close();

        v = new DataPoint(date, Double.parseDouble(arrStr[0]));

        return v;
    }

    private Cursor queryDB() {
        String[] projection = {
                DBContract.RuuvitagDB.COLUMN_HUMI,
                DBContract.RuuvitagDB.COLUMN_LAST
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = DBContract.RuuvitagDB._ID + " = ?";
        String[] selectionArgs = { "1" };

        return db.query(
                DBContract.RuuvitagDB.TABLE_NAME,         // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // dont't sort results
        );
    }*/
}