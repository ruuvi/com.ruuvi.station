package com.ruuvi.station.util.ui

import android.app.Application
import com.ruuvi.station.about.ui.AboutActivityViewModel
import com.ruuvi.station.addtag.ui.AddTagActivityViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.calibration.ui.CalibrateHumidityViewModel
import com.ruuvi.station.calibration.ui.CalibratePressureViewModel
import com.ruuvi.station.calibration.ui.CalibrateTemperatureViewModel
import com.ruuvi.station.settings.ui.AlertNotificationsSettingsViewModel
import com.ruuvi.station.settings.ui.AppSettingsListViewModel
import com.ruuvi.station.settings.ui.AppearanceSettingsViewModel
import com.ruuvi.station.settings.ui.BackgroundScanSettingsViewModel
import com.ruuvi.station.settings.ui.ChartSettingsViewModel
import com.ruuvi.station.settings.ui.CloudSettingsViewModel
import com.ruuvi.station.settings.ui.DataForwardingSettingsViewModel
import com.ruuvi.station.settings.ui.DeveloperSettingsViewModel
import com.ruuvi.station.settings.ui.GlobalUnitsAndResolutionViewModel
import com.ruuvi.station.network.ui.ShareSensorViewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.bindProvider
import org.kodein.di.factory

/** Isolates activity UI tests from production startup, databases, Bluetooth and Firebase. */
class UiTestApplication : Application(), DIAware {
    lateinit var aboutViewModel: AboutActivityViewModel
    lateinit var addTagViewModel: AddTagActivityViewModel
    lateinit var preferences: PreferencesRepository
    lateinit var calibrateHumidityViewModel: CalibrateHumidityViewModel
    lateinit var calibratePressureViewModel: CalibratePressureViewModel
    lateinit var calibrateTemperatureViewModel: CalibrateTemperatureViewModel
    lateinit var alertNotificationsSettingsViewModel: AlertNotificationsSettingsViewModel
    lateinit var appSettingsListViewModel: AppSettingsListViewModel
    lateinit var appearanceSettingsViewModel: AppearanceSettingsViewModel
    lateinit var backgroundScanSettingsViewModel: BackgroundScanSettingsViewModel
    lateinit var chartSettingsViewModel: ChartSettingsViewModel
    lateinit var cloudSettingsViewModel: CloudSettingsViewModel
    lateinit var dataForwardingSettingsViewModel: DataForwardingSettingsViewModel
    lateinit var developerSettingsViewModel: DeveloperSettingsViewModel
    lateinit var globalUnitsAndResolutionViewModel: GlobalUnitsAndResolutionViewModel
    lateinit var shareSensorViewModel: ShareSensorViewModel

    override fun onCreate() {
        super.onCreate()
        com.raizlabs.android.dbflow.config.FlowManager.init(this)
    }

    override val di by DI.lazy {
        bindProvider { aboutViewModel }
        bindProvider { addTagViewModel }
        bindProvider { preferences }
        bind<CalibrateHumidityViewModel>() with factory { _: CalibrationViewModelArgs ->
            calibrateHumidityViewModel
        }
        bind<CalibratePressureViewModel>() with factory { _: CalibrationViewModelArgs ->
            calibratePressureViewModel
        }
        bind<CalibrateTemperatureViewModel>() with factory { _: CalibrationViewModelArgs ->
            calibrateTemperatureViewModel
        }
        bindProvider { alertNotificationsSettingsViewModel }
        bindProvider { appSettingsListViewModel }
        bindProvider { appearanceSettingsViewModel }
        bindProvider { backgroundScanSettingsViewModel }
        bindProvider { chartSettingsViewModel }
        bindProvider { cloudSettingsViewModel }
        bindProvider { dataForwardingSettingsViewModel }
        bindProvider { developerSettingsViewModel }
        bindProvider { globalUnitsAndResolutionViewModel }
        bind<ShareSensorViewModel>() with factory { _: String -> shareSensorViewModel }
    }
}
