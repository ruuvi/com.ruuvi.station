package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.scaledSp


@Composable
fun OtpTextField(
    otpText: String,
    enabled: Boolean = true,
    otpCount: Int = 4,
    modifier: Modifier = Modifier,
    onOtpTextChange: (String, Boolean) -> Unit
) {
    BasicTextField(
        modifier = modifier,
        value = otpText,
        enabled = enabled,
        onValueChange = {
            if (it.length <= otpCount) {
                onOtpTextChange.invoke(it, it.length == otpCount)
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters
        ),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(otpCount) { index ->
                    CharView(
                        index = index,
                        text = otpText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    )
}

@Composable
private fun CharView(
    index: Int,
    text: String
) {
    val isFocused = text.length == index
    val char = when {
        index >= text.length -> ""
        else -> text[index].toString()
    }
    Box {
        Text(
            modifier = Modifier
                .width(charBoxSize)
                .border(
                    1.dp,
                    Color.LightGray,
                    RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            text = char,
            style = RuuviStationTheme.typography.otpChar,
            fontSize = 28.scaledSp,
            textAlign = TextAlign.Center
        )
        if (isFocused) {
            BlinkingEffect {
                Text(
                    modifier = Modifier
                        .width(charBoxSize)
                        .padding(4.dp),
                    text = "_",
                    style = RuuviStationTheme.typography.otpChar,
                    fontSize = 28.scaledSp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private val charBoxSize = 44.dp

@Preview
@Composable
fun OtpTextFieldPreview(modifier: Modifier = Modifier) {
    RuuviTheme {
        OtpTextField(
            otpText = "W7X",
            otpCount = 4,
            enabled = true,
            onOtpTextChange = { _,_ -> },
            modifier = Modifier.background(color = RuuviStationTheme.colors.systemBars)
        )
    }
}