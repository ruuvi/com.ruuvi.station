package com.ruuvi.station.tagsettings.ui.visible_measurements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.Subtitle
import com.ruuvi.station.app.ui.components.SwitchIndicatorRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.dashboard.ui.DashboardItem
import com.ruuvi.station.dashboard.ui.DashboardItemSimple
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tagsettings.ui.SensorSettingsTitle
import com.ruuvi.station.tagsettings.ui.dragGestureHandler
import com.ruuvi.station.tagsettings.ui.rememberDragDropListState
import kotlinx.coroutines.Job
import timber.log.Timber

val testSelected = listOf(
    ListOption("temperature_C", "Temperature Celsius"),
    ListOption("Humidity_0", "Humidity Relative"),
    ListOption("Pressure_1", "Pressure mmHg")
)

val testAllOptions = listOf(
    ListOption("movements", "Movements"),
    ListOption("signal", "Signal strength"),
    ListOption("voltage", "Battery Voltage"),
)

@Composable
fun VisibleMeasurements(
    modifier: Modifier = Modifier,
    sensorState: RuuviTag,
    useDefault: Boolean,
    dashboardType: DashboardType,
    onAction: (VisibleMeasurementsActions) -> Unit,
    selected: List<ListOption>,
    allOptions: List<ListOption>
) {

    Column (
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Paragraph(
            text = stringResource(R.string.visible_measurements_description),
            modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
        )

        SwitchIndicatorRuuvi(
            text = stringResource(id = R.string.visible_measurements_use_default),
            checked = useDefault,
            onCheckedChange = { checked ->
                onAction(VisibleMeasurementsActions.ChangeUseDefault(checked)) },
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
        )

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.small))

        Subtitle(
            text = stringResource(R.string.preview),
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
        )

        Box (
            modifier = Modifier
                .padding(RuuviStationTheme.dimensions.screenPadding)
        ){
            if (dashboardType == DashboardType.IMAGE_VIEW) {
                DashboardItem(
                    lazyGridState = rememberLazyStaggeredGridState(),
                    itemIndex = 0,
                    sensor = sensorState,
                    userEmail = "",
                    displacementOffset = IntOffset.Zero,
                    itemIsDragged = false,
                    setName = {_,_ ->},
                    moveItem = {_,_,_ ->},
                    showMeasurementTitle = true,
                    interactionEnabled = false
                )
            } else {
                DashboardItemSimple(
                    lazyGridState = rememberLazyStaggeredGridState(),
                    itemIndex = 0,
                    sensor = sensorState,
                    userEmail = "",
                    displacementOffset = IntOffset.Zero,
                    itemIsDragged = false,
                    setName = {_,_ ->},
                    moveItem = {_,_,_ ->},
                    interactionEnabled = false
                )
            }
        }

        if (!useDefault) {
            Subtitle(
                text = stringResource(R.string.customisation_settings),
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.screenPadding))

            Paragraph(
                text = stringResource(R.string.customisation_settings_description),
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.screenPadding))

            DragAndDropListEdit(
                selected = selected,
                allOptions = allOptions,
                onMove = { from, to ->
                    onAction(VisibleMeasurementsActions.SwapDisplayOrderItems(from, to))
                },
                onAdd = { listOption ->
                    onAction(VisibleMeasurementsActions.AddToDisplayOrder(listOption.id))
                },
                onRemove = { listOption ->
                    onAction(VisibleMeasurementsActions.RemoveFromDisplayOrder(listOption.id))
                }
            )
        }
    }

}

@Composable
fun DragAndDropListEdit(
    selected: List<ListOption>,
    allOptions: List<ListOption>,
    onMove: (Int, Int) -> Unit,
    onAdd: (ListOption) -> Unit,
    onRemove: (ListOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val dragDropListState = rememberDragDropListState (
        onMove = onMove,
        onDoneDragging = { }
    )

    val itemHeight = 48.dp * LocalDensity.current.fontScale
    val coroutineScope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }

    Column {
        SensorSettingsTitle(stringResource(R.string.show_measurements))
        LazyColumn (
            modifier = Modifier
                .height(itemHeight * selected.size)
                .dragGestureHandler(coroutineScope, dragDropListState, overscrollJob),
            userScrollEnabled = false,
            state = dragDropListState.getLazyListState()
        ) {
            itemsIndexed(selected) { index, listOption ->

                val displacementOffset =
                    if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                        Timber.d("dragGestureHandler - elementDisplacement ${dragDropListState.elementDisplacement}")
                        dragDropListState.elementDisplacement.takeIf { it != 0f }
                    } else {
                        null
                    }
                val itemIsDragged = dragDropListState.getCurrentIndexOfDraggedListItem() == index

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
                        .graphicsLayer {
                            Timber.d("dragGestureHandler - graphicsLayer $displacementOffset")
                            translationY = displacementOffset ?: 0f
                            scaleX = if (itemIsDragged) 1.04f else 1f
                            scaleY = if (itemIsDragged) 1.04f else 1f
                            alpha = if (itemIsDragged) 0.7f else 1f
                        }

                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        style = RuuviStationTheme.typography.subtitle,
                        textAlign = TextAlign.Start,
                        text = listOption.title,
                        maxLines = 2)
                    Icon(
                        painter = painterResource(id = R.drawable.up_down_drag),
                        contentDescription = "drag",
                        tint = RuuviStationTheme.colors.accent.copy(alpha = 0.6f),
                    )
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "remove",
                        tint = RuuviStationTheme.colors.accent,
                        modifier = Modifier
                            .clickable { onRemove(listOption) }
                    )
                }
            }
        }
        SensorSettingsTitle(stringResource(R.string.hide_measurements))
        Column {
            allOptions.forEachIndexed { index, listOption ->
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
                ){
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        style = RuuviStationTheme.typography.subtitle,
                        textAlign = TextAlign.Start,
                        text = listOption.title,
                        maxLines = 2)
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "add",
                        tint = RuuviStationTheme.colors.accent,
                        modifier = Modifier
                            .clickable { onAdd(listOption) }
                    )
                }
            }
        }
    }
}

data class ListOption(
    val id: String,
    val title: String
)

@PreviewLightDark
@Composable
fun VisibleMeasurementsPreview(modifier: Modifier = Modifier) {
    RuuviTheme {
        VisibleMeasurements(
            useDefault = false,
            onAction = {},
            dashboardType = DashboardType.IMAGE_VIEW,
            sensorState = ruuviTagPreview,
            selected = testSelected,
            allOptions = testAllOptions,
            modifier = Modifier.background(RuuviStationTheme.colors.background)
        )
    }
}