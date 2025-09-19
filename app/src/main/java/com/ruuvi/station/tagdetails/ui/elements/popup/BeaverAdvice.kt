package com.ruuvi.station.tagdetails.ui.elements.popup

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.SmallerParagraph
import com.ruuvi.station.app.ui.theme.Elm
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.score.QualityRange
import com.ruuvi.station.units.domain.score.ScoreAqi
import com.ruuvi.station.units.domain.score.ScoreCo2
import com.ruuvi.station.units.domain.score.ScorePM
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@Composable
fun BeaverAdvice(
    advice: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Elm,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Row (verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.beaver_nofloor_200),
                contentDescription = "Beaver",
                modifier = Modifier.size(85.dp),
                contentScale = ContentScale.Fit
            )

            SmallerParagraph(
                text = advice,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun BeaverAdvicePreview() {
    RuuviTheme {
        Box (
            Modifier
                .background(RuuviStationTheme.colors.popupBackground)
                .padding(32.dp)
        ) {
            BeaverAdvice(stringResource(AQI_EXCELLENT.random()))
        }
    }
}

private val AQI_EXCELLENT = intArrayOf(
    R.string.aqi_advice_excellent1,
    R.string.aqi_advice_excellent2,
    R.string.aqi_advice_excellent3,
    R.string.aqi_advice_excellent4,
    R.string.aqi_advice_excellent5,
    R.string.aqi_advice_excellent6
)
private val AQI_GOOD = intArrayOf(
    R.string.aqi_advice_good1,
    R.string.aqi_advice_good2,
    R.string.aqi_advice_good3,
    R.string.aqi_advice_good4,
    R.string.aqi_advice_good5,
    R.string.aqi_advice_good6
)
private val AQI_FAIR = intArrayOf(
    R.string.aqi_advice_fair1,
    R.string.aqi_advice_fair2,
    R.string.aqi_advice_fair3,
    R.string.aqi_advice_fair4,
    R.string.aqi_advice_fair5,
    R.string.aqi_advice_fair6
)
private val AQI_POOR = intArrayOf(
    R.string.aqi_advice_poor1,
    R.string.aqi_advice_poor2,
    R.string.aqi_advice_poor3,
    R.string.aqi_advice_poor4,
    R.string.aqi_advice_poor5,
    R.string.aqi_advice_poor6
)
private val AQI_VERYPOOR = intArrayOf(
    R.string.aqi_advice_verypoor1,
    R.string.aqi_advice_verypoor2,
    R.string.aqi_advice_verypoor3,
    R.string.aqi_advice_verypoor4,
    R.string.aqi_advice_verypoor5,
    R.string.aqi_advice_verypoor6
)

private fun aqiSet(range: QualityRange): IntArray = when (range) {
    is QualityRange.Excellent -> AQI_EXCELLENT
    is QualityRange.Good      -> AQI_GOOD
    is QualityRange.Fair      -> AQI_FAIR
    is QualityRange.Poor      -> AQI_POOR
    is QualityRange.VeryPoor  -> AQI_VERYPOOR
}

@StringRes
private fun co2PmResId(
    co2: QualityRange?,
    pm: QualityRange?
): Int? = when (co2) {
    is QualityRange.Excellent -> when (pm) {
        is QualityRange.Good      -> R.string.aqi_advice_co2_excellent_pm_good
        is QualityRange.Fair      -> R.string.aqi_advice_co2_excellent_pm_fair
        is QualityRange.Poor      -> R.string.aqi_advice_co2_excellent_pm_poor
        is QualityRange.VeryPoor  -> R.string.aqi_advice_co2_excellent_pm_verypoor
        else -> null
    }
    is QualityRange.Good -> when (pm) {
        is QualityRange.Excellent -> R.string.aqi_advice_co2_good_pm_excellent
        is QualityRange.Good      -> R.string.aqi_advice_co2_good_pm_good
        is QualityRange.Fair      -> R.string.aqi_advice_co2_good_pm_fair
        is QualityRange.Poor      -> R.string.aqi_advice_co2_good_pm_poor
        is QualityRange.VeryPoor  -> R.string.aqi_advice_co2_good_pm_verypoor
        else -> null
    }
    is QualityRange.Fair -> when (pm) {
        is QualityRange.Excellent -> R.string.aqi_advice_co2_fair_pm_excellent
        is QualityRange.Good      -> R.string.aqi_advice_co2_fair_pm_good
        is QualityRange.Fair      -> R.string.aqi_advice_co2_fair_pm_fair
        is QualityRange.Poor      -> R.string.aqi_advice_co2_fair_pm_poor
        is QualityRange.VeryPoor  -> R.string.aqi_advice_co2_fair_pm_verypoor
        else -> null
    }
    is QualityRange.Poor -> when (pm) {
        is QualityRange.Excellent -> R.string.aqi_advice_co2_poor_pm_excellent
        is QualityRange.Good      -> R.string.aqi_advice_co2_poor_pm_good
        is QualityRange.Fair      -> R.string.aqi_advice_co2_poor_pm_fair
        is QualityRange.Poor      -> R.string.aqi_advice_co2_poor_pm_poor
        is QualityRange.VeryPoor  -> R.string.aqi_advice_co2_poor_pm_verypoor
        else -> null
    }
    is QualityRange.VeryPoor -> when (pm) {
        is QualityRange.Excellent -> R.string.aqi_advice_co2_verypoor_pm_excellent
        is QualityRange.Good      -> R.string.aqi_advice_co2_verypoor_pm_good
        is QualityRange.Fair      -> R.string.aqi_advice_co2_verypoor_pm_fair
        is QualityRange.Poor      -> R.string.aqi_advice_co2_verypoor_pm_poor
        is QualityRange.VeryPoor  -> R.string.aqi_advice_co2_verypoor_pm_verypoor
        else -> null
    }
    else -> null
}

fun getBeaverAdvice(
    context: Context,
    aqi: EnvironmentValue,
    extraValues: List<EnvironmentValue>
): String {
    val co2 = extraValues.firstOrNull{it.unitType == UnitType.CO2.Ppm}
    val pm25 = extraValues.firstOrNull{it.unitType == UnitType.PM.PM25}
    val aqiScore = ScoreAqi.score(aqi.value)
    val scoreCo2 = co2?.value?.let { ScoreCo2.score(it) }
    val scorePm25 = pm25?.value?.let { ScorePM.score(it) }

    val aqiIds = aqiSet(aqiScore)
    val aqiResId = aqiIds.random()

    val addString = co2PmResId(scoreCo2, scorePm25)

    return context.getString(aqiResId) +
            (addString?.let { "\n\n" + context.getString(it) } ?: "")
}