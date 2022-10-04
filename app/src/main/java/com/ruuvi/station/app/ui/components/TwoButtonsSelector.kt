package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import timber.log.Timber

@Composable
fun StartRoundedButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    RuuviButton(
        text = text,
        modifier = modifier
            .width(RuuviStationTheme.dimensions.buttonWidth),
        height = RuuviStationTheme.dimensions.buttonHeightSmall,
        shape = RoundedCornerShape(topStartPercent = 40, bottomStartPercent = 40),
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
fun EndRoundedButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    RuuviButton(
        text = text,
        modifier = modifier
            .width(RuuviStationTheme.dimensions.buttonWidth),
        height = RuuviStationTheme.dimensions.buttonHeightSmall,
        shape = RoundedCornerShape(topEndPercent = 40, bottomEndPercent = 40),
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
fun TwoButtonsSelector(
    values: List<SelectionElement>,
    initialValue: SelectionElement,
    onValueChanged: (Int) -> Unit
) {
    val selectedIndex = remember {
        var index = values.indexOf(initialValue)
        if (index == -1) {
            index = 0
        }
        mutableStateOf(index)
    }

    var selectedElement = values[selectedIndex.value]

    fun valueChanged() {
        selectedElement = values[selectedIndex.value]
        Timber.d("TwoButtonsSelector $selectedIndex $selectedElement")
        onValueChanged.invoke(selectedElement.value)
    }

    TwoButtonsElement(
        text = stringResource(id = selectedElement.captionResource, selectedElement.resourceArgument),
        onMinusClick = {
            if (selectedIndex.value > 0) {
                selectedIndex.value--
                valueChanged()
            }
        },
        onPlusClick = {
            if (selectedIndex.value < values.size - 1) {
                selectedIndex.value++
                valueChanged()
            }
        }
    )


}

@Composable
fun TwoButtonsElement (
    text: String,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Paragraph(text = text)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .width(IntrinsicSize.Min)
        ) {
            StartRoundedButton(text = "-") { onMinusClick.invoke() }
            Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.minimal))
            EndRoundedButton(text = "+") { onPlusClick.invoke() }
        }
    }
}

data class SelectionElement(
    val value: Int,
    val resourceArgument: Int,
    val captionResource: Int
)