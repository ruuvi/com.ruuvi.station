package com.ruuvi.station.tagdetails.ui.elements.popup

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.SmallerParagraph
import com.ruuvi.station.app.ui.components.rememberResourceUri
import com.ruuvi.station.app.ui.theme.Elm
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
            //.clip(RoundedCornerShape(5.dp))
            .border(
                width = 1.dp,
                color = Elm,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Row (verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = rememberResourceUri(R.drawable.beaver_nofloor_200),
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

@Preview
@Composable
fun BeaverAdvicePreview() {
    RuuviTheme {
        BeaverAdvice("Have a nice day!")
    }
}

fun getBeaverAdvice(
    aqi: EnvironmentValue,
    extraValues: List<EnvironmentValue>
): Int? {
    val aqiScore = ScoreAqi.score(aqi.value)
    val co2 = extraValues.firstOrNull{it.unitType == UnitType.CO2.Ppm}
    val pm25 = extraValues.firstOrNull{it.unitType == UnitType.PM.PM25}
    val scoreCo2 = co2?.value?.let { ScoreCo2.score(it) }
    val scorePm25 = pm25?.value?.let { ScorePM.score(it) }
    return when (aqiScore) {
        QualityRange.Excellent -> R.string.aqi_advice_excellent
        QualityRange.Good -> R.string.aqi_advice_good
        QualityRange.Fair -> {
            if (scoreCo2 == QualityRange.Fair) {
                if (scorePm25 == QualityRange.Fair) {
                    R.string.aqi_advice_moderate_both
                } else {
                    R.string.aqi_advice_moderate_co2
                }
            } else {
                R.string.aqi_advice_moderate_pm25
            }
        }
        QualityRange.Poor -> {
            if (scoreCo2 == QualityRange.Poor) {
                if (scorePm25 == QualityRange.Poor) {
                    R.string.aqi_advice_poor_both
                } else {
                    R.string.aqi_advice_poor_co2
                }
            } else {
                R.string.aqi_advice_poor_pm25
            }
        }
        QualityRange.VeryPoor -> {
            if (scoreCo2 == QualityRange.VeryPoor) {
                if (scorePm25 == QualityRange.VeryPoor) {
                    R.string.aqi_advice_verypoor_both
                } else {
                    R.string.aqi_advice_verypoor_co2
                }
            } else {
                R.string.aqi_advice_verypoor_pm25
            }
        }
        else -> null
    }
}