package com.ruuvi.station.calibration.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ruuvi.station.R
import com.ruuvi.station.databinding.ViewCalibrationMenuitemBinding

class CalibrationMenuItem @JvmOverloads
constructor(
    ctx: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(ctx, attributeSet, defStyleAttr){

    private var binding: ViewCalibrationMenuitemBinding

    init {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewCalibrationMenuitemBinding.inflate(inflater, this)

        val attributes = ctx.obtainStyledAttributes(attributeSet, R.styleable.CalibrationMenuItem)

        val title = attributes.getString(R.styleable.CalibrationMenuItem_itemTitle)
        val value = attributes.getString(R.styleable.CalibrationMenuItem_itemValue)
        attributes.recycle()

        binding.itemTitleTextView.text = title
        binding.itemValueTextView.text = value
    }

    fun setItemValue(newValue: String) {
        binding.itemValueTextView.text = newValue
    }
}