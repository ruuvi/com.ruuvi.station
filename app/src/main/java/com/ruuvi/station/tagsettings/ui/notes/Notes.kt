package com.ruuvi.station.tagsettings.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.ruuvi.station.R

@Composable
fun Notes(
    modifier: Modifier = Modifier,
    note: String,
    onAction: (NotesActions) -> Unit,
    effects: SharedFlow<NotesEffect>,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is NotesEffect.NoteUpdated -> onNavigateBack()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.screenPadding))

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 2.dp,
                    color = RuuviStationTheme.colors.accent,
                    shape = RoundedCornerShape(RuuviStationTheme.dimensions.extended)
                ),
            value = note,
            onValueChange = { onAction(NotesActions.EditNote(it)) },
            shape = RoundedCornerShape(RuuviStationTheme.dimensions.extended),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = RuuviStationTheme.colors.accent,
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                backgroundColor = RuuviStationTheme.colors.background
            ),
            textStyle = RuuviStationTheme.typography.paragraph
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RuuviStationTheme.dimensions.small),
            text = "${note.length}/1000",
            style = RuuviStationTheme.typography.paragraphSmall,
            textAlign = TextAlign.End,
            color = RuuviStationTheme.colors.secondaryTextColor
        )

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

        RuuviButton(
            text = stringResource(id = R.string.update),
            onClick = { onAction(NotesActions.UpdateNote) }
        )

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.screenPadding))
    }
}

@PreviewLightDark
@Composable
fun NotesPreview() {
    RuuviTheme {
        Notes(
            note = "1. \n2. \n3. \n4. \n5. ",
            onAction = {},
            effects = MutableSharedFlow(),
            onNavigateBack = {},
            modifier = Modifier.background(RuuviStationTheme.colors.background)
        )
    }
}