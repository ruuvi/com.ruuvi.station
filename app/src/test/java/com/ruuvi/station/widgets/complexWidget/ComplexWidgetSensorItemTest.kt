package com.ruuvi.station.widgets.complexWidget

import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferenceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplexWidgetSensorItemTest {
    private val sensor = ruuviTagPreview.copy(
        displayOrder = WidgetType.entries.map { it.unitType },
        possibleDisplayOptions = emptyList(),
    )

    @Test
    fun `all supported measurements survive preference conversion and restore`() {
        val supportedTypes = WidgetType.filterWidgetTypes(sensor)
        assertTrue(supportedTypes.size > 9)

        val selectedItem = ComplexWidgetSensorItem(sensor).apply {
            checked = true
            supportedTypes.forEach { setStateForType(it, true) }
        }
        val savedSettings = ComplexWidgetPreferenceItem(selectedItem)
        val restoredItem = ComplexWidgetSensorItem(sensor)

        restoredItem.restoreSettings(sensor, savedSettings)

        assertTrue(restoredItem.checked)
        assertEquals(
            supportedTypes,
            supportedTypes.filter(restoredItem::getStateForType),
        )
    }
}
