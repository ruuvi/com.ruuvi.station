package com.ruuvi.station.app.ui

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.kodein.di.DKodein
import org.kodein.di.generic.instanceOrNull

class ViewModelFactory(private val injector: DKodein) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return injector.instanceOrNull<ViewModel>(tag = modelClass.simpleName) as T?
                ?: modelClass.newInstance()
    }
}