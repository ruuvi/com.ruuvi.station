package com.ruuvi.station.units.model

import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.domain.score.QualityCalculator

/** Shared presentation for indices whose labels and scale direction can differ. */
data class IndexPresentation(
    val score: Double?,
    val scoreString: String,
    val maximum: Int,
    val color: Color,
    val title: Int,
    val description: Int,
    val icon: Int
)

fun AQI.toIndexPresentation() = IndexPresentation(
    score, scoreString, 100, color, R.string.air_quality, descriptionRes, R.drawable.icon_air_quality
)

fun EnvironmentValue.mouldRiskPresentation(): IndexPresentation {
    require(unitType == UnitType.MouldRisk.Index)
    val quality = QualityCalculator.calc(this)
    return IndexPresentation(
        score = value.takeIf { isAvailable },
        scoreString = valueWithoutUnit.substringBefore('/').trim(),
        maximum = 100,
        color = quality?.color ?: Color.Gray,
        title = unitType.measurementTitle,
        description = quality?.description ?: R.string.mould_risk_unavailable,
        icon = unitType.iconRes
    )
}

fun EnvironmentValue.mouldRiskPresentationOrNull(): IndexPresentation? =
    if (unitType == UnitType.MouldRisk.Index) mouldRiskPresentation() else null
