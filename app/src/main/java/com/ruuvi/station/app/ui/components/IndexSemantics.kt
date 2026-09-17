package com.ruuvi.station.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ruuvi.station.units.model.IndexPresentation

@Composable
fun Modifier.indexSemantics(index: IndexPresentation?): Modifier {
    if (index == null) return this
    val category = stringResource(index.description)
    return semantics(mergeDescendants = true) { stateDescription = category }
}
