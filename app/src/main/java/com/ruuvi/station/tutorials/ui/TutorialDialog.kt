package com.ruuvi.station.tutorials.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.RuuviCheckbox
import com.ruuvi.station.app.ui.components.Title
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.tutorials.Tutorial
import com.ruuvi.station.tutorials.preferences.TutorialPreferences
import timber.log.Timber

@Composable
fun TutorialDialog(
    tutorial: Tutorial,
) {
    val context = LocalContext.current
    val prefs = remember { TutorialPreferences(context) }

    val shouldShow by prefs
        .shouldShow(tutorial.preferenceKey)
        .collectAsState(initial = false)

    var dismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var doNotShow by rememberSaveable { mutableStateOf(false) }

    val open = shouldShow && !dismissedThisSession

    Timber.d("TutorialDialog $open")
    if (open) {
        Dialog(
            onDismissRequest = {
                Timber.d("TutorialDialog onDismissRequest $doNotShow")
                dismissedThisSession = true
                if (doNotShow) prefs.setDontShowAgain(tutorial.preferenceKey)
            },
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(RuuviStationTheme.dimensions.medium),
                backgroundColor = RuuviStationTheme.colors.background
            )
            {
                Column(
                    modifier = Modifier
                        .padding(all = RuuviStationTheme.dimensions.extended)
                ) {
                    tutorial.body()
                    Spacer(Modifier.height(32.dp))
                    RuuviCheckbox(
                        checked = doNotShow,
                        text = stringResource(R.string.do_not_show_again)
                    ) {
                        doNotShow = !doNotShow
                    }

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            dismissedThisSession = true
                            if (doNotShow) prefs.setDontShowAgain(tutorial.preferenceKey)
                        }
                    ) {
                        Title(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}