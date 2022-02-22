package com.ruuvi.station.util.ui

import android.content.Context
import android.util.AttributeSet
import com.ruuvi.station.R
import timber.log.Timber

class SequenceNumberEdit @JvmOverloads
constructor(
    private val ctx: Context,
    private val attributeSet: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : BasePlusMinusEdit(ctx, attributeSet, defStyleAttr) {

    private var minValue: Int = MIN_VALUE_DEFAULT
    private var maxValue: Int = MAX_VALUE_DEFAULT
    private var captionResourceId: Int? = null

    private var _selectedValue: Int = minValue
    var selectedValue: Int
        get() = _selectedValue
        set(value) {
            _selectedValue = when {
                value < minValue -> {
                    minValue
                }
                value > maxValue -> {
                    maxValue
                }
                else -> {
                    value
                }
            }
            refreshCaption()
            valueChangedListener?.invoke(selectedValue)
        }

    private fun refreshCaption() {
        val resource = captionResourceId
        val caption = if (resource == null) {
            selectedValue.toString()
        } else {
            ctx.getString(resource, selectedValue.toString())
        }
        binding.captionTextView.text = caption
    }

    override fun increment() {
        selectedValue += 1
    }

    override fun decrement() {
        selectedValue -= 1
    }

    init {
        Timber.d("BasePlusMinusEdit - Number init")
        val attributes = ctx.obtainStyledAttributes(attributeSet, R.styleable.SequenceNumberEdit)

        minValue = attributes.getInt(R.styleable.SequenceNumberEdit_minValue, MIN_VALUE_DEFAULT)
        maxValue = attributes.getInt(R.styleable.SequenceNumberEdit_maxValue, MAX_VALUE_DEFAULT)

        val captionResource = attributes.getResourceId(R.styleable.SequenceNumberEdit_captionResource, -1)
        captionResourceId = if (captionResource != -1) captionResource else null

        attributes.recycle()
    }

    companion object {
        const val MIN_VALUE_DEFAULT = 1
        const val MAX_VALUE_DEFAULT = 60
    }
}