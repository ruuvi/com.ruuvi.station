package com.ruuvi.station.tagdetails.di

import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.tagdetails.domain.TagDetailsArguments
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagdetails.ui.TagDetailsViewModel
import com.ruuvi.station.tagdetails.ui.TagViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

@ExperimentalCoroutinesApi
object TagDetailsInjectionModule {
    val module = Kodein.Module(TagDetailsInjectionModule.javaClass.name) {

        bind<TagDetailsInteractor>() with singleton {
            TagDetailsInteractor(instance(), instance())
        }

        bind<TagDetailsViewModel>() with factory { args: TagDetailsArguments ->
            TagDetailsViewModel(args, instance(), instance())
        }

        bind<TagViewModel>() with factory { args: TagViewModelArgs ->
            TagViewModel(instance(), tagId = args.tagId)
        }

        bind<TagRepository>() with singleton {
            TagRepository(instance(), instance())
        }
    }
}