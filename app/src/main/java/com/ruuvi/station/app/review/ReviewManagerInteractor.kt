package com.ruuvi.station.app.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ReviewManagerInteractor(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val sensorSettingsRepository: SensorSettingsRepository
) {
    val manager = ReviewManagerFactory.create(context)

    private fun shouldAskForReview(): Boolean {
        val installDate =
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        val installDurationDays = abs(TimeUnit.MILLISECONDS.toDays(Date().time - installDate))
        if (installDurationDays < intervalAfterInstallDays) return false

        val lastRequestDate =
            if (preferencesRepository.getRequestForReviewDate() == Long.MIN_VALUE) 0 else preferencesRepository.getRequestForReviewDate()
        val lastRequestDurationDays =
            abs(TimeUnit.MILLISECONDS.toDays(Date().time - Date(lastRequestDate).time))
        if (lastRequestDurationDays < intervalBetweenRequestsDays) return false

        val sensorsCount = sensorSettingsRepository.getSensorSettings().size
        return sensorsCount >= minimumSensorsCount
    }

    fun requestReview(activity: Activity) {
        val shouldAskForReview = shouldAskForReview()
        Timber.d("shouldAskForReview = $shouldAskForReview")
        if (!shouldAskForReview()) return
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                preferencesRepository.updateRequestForReviewDate()
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                }
            } else {
                Timber.e(task.exception)
            }
        }
    }

    companion object {
        //TODO these values are for testing
        const val intervalAfterInstallDays = 0
        const val intervalBetweenRequestsDays = 0
        const val minimumSensorsCount = 1
    }
}