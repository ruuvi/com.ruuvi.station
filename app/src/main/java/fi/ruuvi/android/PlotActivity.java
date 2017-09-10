package fi.ruuvi.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


import fi.ruuvi.android.model.RuuviTag;
import fi.ruuvi.android.util.PlotSource;

public class PlotActivity extends AppCompatActivity {
    private XYPlot temp_plot;
    private XYPlot hum_plot;
    private XYPlot pres_plot;

    private PlotSource plotSource;
    private Date[] domains;
    private String id;
    private String name;

    Number temp[];
    Number humidity[];
    Number pressure[];

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        intent = getIntent();
        String[] extras = (String[]) intent.getExtras().get("id");
        id = extras[0];
        name = extras[1];

        setTitle("Graphs for " + name);

        plotSource = PlotSource.getInstance();
        domains = plotSource.getDomains();
        temp_plot = (XYPlot) findViewById(R.id.plotTemperature);
        hum_plot = (XYPlot) findViewById(R.id.plotHumidity);
        pres_plot = (XYPlot) findViewById(R.id.plotPressure);

        RuuviTag[] series = plotSource.getSeriesForTag(id);

        temp = new Number[series.length];
        humidity = new Number[series.length];
        pressure = new Number[series.length];
        for(int i= 0; i < series.length; i++)
        {
            if(series[i] != null)
            {
                temp[i] = series[i].temperature;
                humidity[i] = series[i].humidity;
                pressure[i] = series[i].pressure;
            }
        }
    }

    private void makeTemperaturePlot()
    {
        temp_plot.setRangeBoundaries(-30,90, BoundaryMode.FIXED);
        LineAndPointFormatter series1Format =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        XYSeries series1 = new SimpleXYSeries(Arrays.asList(temp), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Temperature");
        temp_plot.addSeries(series1, series1Format);
        temp_plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                int i = Math.round(((Number) obj).floatValue());
                SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm a");

                return toAppendTo.append(dt1.format(domains[i]));
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
        temp_plot.getGraph().setSize(new Size(0,SizeMode.FILL,0,SizeMode.FILL));
        temp_plot.getGraph().setPaddingLeft(75.0f);
        temp_plot.getLayoutManager().remove(temp_plot.getDomainTitle());
        temp_plot.getLayoutManager().remove(temp_plot.getTitle());
        temp_plot.getLayoutManager().remove(temp_plot.getRangeTitle());
    }

    @Override
    protected void onResume() {
        super.onResume();
        makePlots();
    }

    private void makePlots() {
        makeTemperaturePlot();
        makeHumidityPlot();
        makePressurePlot();
    }

    private void makeHumidityPlot()
    {
        hum_plot.setRangeBoundaries(0,100, BoundaryMode.FIXED);
        LineAndPointFormatter series1Format =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        XYSeries series1 = new SimpleXYSeries(Arrays.asList(humidity), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Humidity");
        hum_plot.addSeries(series1, series1Format);
        hum_plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                int i = Math.round(((Number) obj).floatValue());
                SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm a");

                return toAppendTo.append(dt1.format(domains[i]));
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
        hum_plot.getGraph().setSize(new Size(0,SizeMode.FILL,0,SizeMode.FILL));
        hum_plot.getGraph().setPaddingLeft(75.0f);
        hum_plot.getLayoutManager().remove(hum_plot.getDomainTitle());
        hum_plot.getLayoutManager().remove(hum_plot.getTitle());
        hum_plot.getLayoutManager().remove(hum_plot.getRangeTitle());
    }

    private void makePressurePlot()
    {
        pres_plot.setRangeBoundaries(800,1200, BoundaryMode.FIXED);
        LineAndPointFormatter series1Format =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        XYSeries series1 = new SimpleXYSeries(Arrays.asList(pressure), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Pressure");
        pres_plot.addSeries(series1, series1Format);
        pres_plot.getGraph().setSize(new Size(0,SizeMode.FILL,0,SizeMode.FILL));
        pres_plot.getGraph().setPaddingLeft(75.0f);
        pres_plot.getLayoutManager().remove(pres_plot.getDomainTitle());
        pres_plot.getLayoutManager().remove(pres_plot.getTitle());
        pres_plot.getLayoutManager().remove(pres_plot.getRangeTitle());

        pres_plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                int i = Math.round(((Number) obj).floatValue());
                SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm a");

                return toAppendTo.append(dt1.format(domains[i]));
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_update) {
            // A bit of a hack
            startActivity(intent);
            overridePendingTransition(0, 0);
            this.finish();
            overridePendingTransition(0, 0);
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph, menu);
        return true;
    }
}
