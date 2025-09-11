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