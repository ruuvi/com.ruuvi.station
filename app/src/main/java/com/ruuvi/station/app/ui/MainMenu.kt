package com.ruuvi.station.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun MainMenu(
    items: List<MenuItem>,
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn() {
        itemsIndexed(items) { index, item ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemClick.invoke(item)
                    }
                    .padding(
                        top = RuuviStationTheme.dimensions.mediumPlus,
                        bottom = RuuviStationTheme.dimensions.mediumPlus,
                        start = RuuviStationTheme.dimensions.extended,
                        end = RuuviStationTheme.dimensions.extended
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = item.title, style = RuuviStationTheme.typography.menuItem)
            }
            if (index < items.lastIndex) {
                DividerRuuvi()
            }
        }
    }
}

data class MenuItem(
    val id: Int,
    val title: String
)