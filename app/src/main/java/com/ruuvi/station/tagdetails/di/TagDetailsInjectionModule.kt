package com.ruuvi.station.tagdetails.di

import com.ruuvi.station.export.CsvExporter
import com.ruuvi.station.export.ExportDataPreparator
import com.ruuvi.station.export.XlsxExporter
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagdetails.domain.TagViewModelArgs
import com.ruuvi.station.tagdetails.ui.SensorCardViewModel
import com.ruuvi.station.tagdetails.ui.SensorCardViewModelArguments
import com.ruuvi.station.tagdetails.ui.TagViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object TagDetailsInjectionModule {
    val module = Kodein.Module(TagDetailsInjectionModule.javaClass.name) {

        bind<TagDetailsInteractor>() with singleton {
            TagDetailsInteractor(instance(), instance(), instance(), instance(), instance())
        }

        bind<TagViewModel>() with factory { args: TagViewModelArgs ->
            TagViewModel(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), sensorId = args.tagId)
        }

        bind<SensorCardViewModel>() with factory { arguments: SensorCardViewModelArguments ->
            SensorCardViewModel(arguments, instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<ExportDataPreparator>() with singleton { ExportDataPreparator(instance(), instance(), instance(), instance(), instance()) }

        bind<CsvExporter>() with singleton { CsvExporter(instance(), instance()) }

        bind<XlsxExporter>() with singleton { XlsxExporter(instance(), instance()) }
    }
}