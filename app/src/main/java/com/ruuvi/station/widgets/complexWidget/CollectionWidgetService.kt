package com.ruuvi.station.widgets.complexWidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import timber.log.Timber

class CollectionWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        Timber.d("CollectionWidgetService $this $appWidgetId")

        return CollectionRemoteViewsFactory(applicationContext, appWidgetId)
    }
}