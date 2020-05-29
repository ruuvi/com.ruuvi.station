package com.ruuvi.station.tagdetails.di

import android.arch.lifecycle.ViewModel
import com.ruuvi.station.tagdetails.ui.TagDetailsViewModel
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagdetails.ui.TagViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object TagDetailsInjectionModule {
    val module = Kodein.Module(TagDetailsInjectionModule.javaClass.name) {
        bind<TagDetailsInteractor>() with singleton { TagDetailsInteractor() }

        bind<ViewModel>(tag = TagDetailsViewModel::class.java.simpleName) with provider { TagDetailsViewModel(instance(), instance()) }

        bind<ViewModel>(tag = TagViewModel::class.java.simpleName) with provider { TagViewModel(instance()) }
    }
}