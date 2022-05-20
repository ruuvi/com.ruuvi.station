package com.ruuvi.station.widgets.complexWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.ruuvi.station.R
import com.ruuvi.station.widgets.data.ComplexWidgetData
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferenceItem
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class CollectionWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        Timber.d("CollectionWidgetService $this $appWidgetId")

        return CollectionRemoteViewsFactory(applicationContext, appWidgetId)
    }

    class CollectionRemoteViewsFactory (
        private val context: Context,
        private val widgetAppId: Int
    ): RemoteViewsFactory, KodeinAware {

        override val kodein: Kodein by kodein(context)

        private val interactor: WidgetInteractor by instance()

        private val preferencesInteractor: ComplexWidgetPreferencesInteractor by instance()

        data class WidgetItem(val text: String, val sensorId: String)

        private var widgetItems: List<WidgetItem> = emptyList()

        private var widgetSettings: List<ComplexWidgetPreferenceItem> = emptyList()

        override fun onCreate() {}

        override fun onDestroy() {}

        override fun onDataSetChanged() {
            Timber.d("onDataSetChanged")
            widgetSettings = preferencesInteractor.getComplexWidgetSettings(widgetAppId)
            Timber.d("$widgetSettings")
            widgetItems = interactor.getCloudSensorsList()
                .filter { cloudSensor -> widgetSettings.any { it.sensorId ==  cloudSensor.id} }
                .map { WidgetItem(it.displayName, it.id) }
        }

        override fun getCount(): Int {
            return widgetItems.size
        }

        override fun getViewAt(position: Int): RemoteViews {
            // Construct a remote views item based on the widget item XML file,
            // and set the text based on the position.
            Timber.d("getViewAt $position")
            val sensorId = widgetItems[position].sensorId
            var data: ComplexWidgetData
            val widgetSettings = widgetSettings.firstOrNull { it.sensorId == sensorId }
            runBlocking(Dispatchers.Main){
                data = interactor.getComplexWidgetData(sensorId, widgetSettings)
            }

            return RemoteViews(context.packageName, R.layout.widget_complex_item).apply {
                setTextViewText(R.id.sensorNameTextView, data.displayName)
                setTextViewText(R.id.updatedTextView, data.updated)

                //TODO FIX HIGHLIGHT ISSUE
                //Resources.Theme(R.style.AppTheme_AppWidgetContainer)
//                var typedValue = TypedValue()
//                context.theme.resolveAttribute(R.attr.colorControlHighlight, typedValue, true)
//                Timber.d("typedValue $typedValue")
                if (position.mod(2) == 0) {
                    setInt(R.id.rootLayout, "setBackgroundResource", Color.TRANSPARENT)
                }
//                else {
//                    setInt(R.id.rootLayout, "setBackgroundResource", typedValue.data)
//                }

                var lastFilledIndex = 0
                for ((index, controls) in valuesControls.withIndex()) {
                    val sensorValue = data.sensorValues.elementAtOrNull(index)
                    if (sensorValue != null) {
                        setViewVisibility(controls.first, View.VISIBLE)
                        setTextViewText(controls.second, sensorValue.sensorValue)
                        setTextViewText(controls.third, sensorValue.unit)
                        lastFilledIndex = index
                    } else {
                        if ((lastFilledIndex >= 0 && index <= 2) || (lastFilledIndex > 2 && index <= 5) || (lastFilledIndex > 5 && index <=8)) {
                            setViewVisibility(controls.first, View.INVISIBLE)
                        } else {
                            setViewVisibility(controls.first, View.GONE)
                        }
                    }
                }
            }
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return widgetItems[position].hashCode().toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        companion object {
            private val valuesControls = listOf(
                Triple(R.id.layout1, R.id.valueTextView1, R.id.unitTextView1),
                Triple(R.id.layout2, R.id.valueTextView2, R.id.unitTextView2),
                Triple(R.id.layout3, R.id.valueTextView3, R.id.unitTextView3),
                Triple(R.id.layout4, R.id.valueTextView4, R.id.unitTextView4),
                Triple(R.id.layout5, R.id.valueTextView5, R.id.unitTextView5),
                Triple(R.id.layout6, R.id.valueTextView6, R.id.unitTextView6),
                Triple(R.id.layout7, R.id.valueTextView7, R.id.unitTextView7),
                Triple(R.id.layout8, R.id.valueTextView8, R.id.unitTextView8),
                Triple(R.id.layout9, R.id.valueTextView9, R.id.unitTextView9),
            )
        }
    }
}