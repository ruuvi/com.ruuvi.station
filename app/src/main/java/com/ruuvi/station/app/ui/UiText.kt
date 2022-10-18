package com.ruuvi.station.app.ui

import android.content.Context

sealed class UiText {
    data class DynamicString(val text: String): UiText()
    data class StringResource(val resId: Int): UiText()
    data class StringResourceWithArgs(val resId: Int, val formatArgs: Array<Any>): UiText()
    class EmptyString: UiText()

    fun asString(context: Context): String {
        return when(this) {
            is DynamicString -> text
            is StringResource -> context.getString(resId)
            is StringResourceWithArgs -> context.getString(resId, *formatArgs)
            is EmptyString -> ""
        }
    }
}