package com.ruuvi.station.util.ui

import android.content.Context
import android.util.AttributeSet
import kotlin.math.abs

class CustomNumberEdit @JvmOverloads
constructor(
    private val ctx: Context,
    private val attributeSet: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : BasePlusMinusEdit(ctx, attributeSet, defStyleAttr) {

    var elements: List<SelectionElement> = listOf()

    private var _selelectedIndex: Int = -1
    private var selelectedIndex
        get() = _selelectedIndex
        set(value) {
            _selelectedIndex = value
            refreshCaption()
            valueChangedListener?.invoke(elements[value].value)
        }

    fun setSelectedItem(value: Int) {
        val exactIndex = elements.indexOfFirst { selectionElement -> selectionElement.value == value }
        if (exactIndex == -1) {
            val closestElement = elements.minByOrNull { abs(it.value - value) }!!
            selelectedIndex = elements.indexOf(closestElement)
        } else {
            selelectedIndex = exactIndex
        }
    }

    fun refreshCaption() {
        val selectedElement = elements.elementAtOrNull(selelectedIndex)
        if (selectedElement != null) {
            val caption = if (selectedElement.resourceArgument != null) {
                ctx.getString(selectedElement.captionResource, selectedElement.resourceArgument)
            } else {
                ctx.getString(selectedElement.captionResource)
            }

            binding.captionTextView.text = caption
        }
    }

    override fun increment() {
        if (selelectedIndex < elements.count() - 1)
            selelectedIndex += 1
    }

    override fun decrement() {
        if (selelectedIndex > 0)
            selelectedIndex -= 1
    }

    data class SelectionElement(
        val value: Int,
        val resourceArgument: Int?,
        val captionResource: Int
    )
}