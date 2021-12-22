package com.ruuvi.station.util.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.ruuvi.station.databinding.ViewNumberPickerBinding
import kotlinx.coroutines.*
import timber.log.Timber

abstract class BasePlusMinusEdit constructor(
    private val ctx: Context,
    private val attributeSet: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : LinearLayout(ctx, attributeSet, defStyleAttr) {

    var binding: ViewNumberPickerBinding

    private var longClickJob: Job? = null

    open var valueChangedListener: ((Int)->Unit)? = null

    open fun increment() {}

    open fun decrement() {}

    private fun setupUI() {
        binding.minusButton.setOnClickListener {
            if (longClickRunning()) {
                stopLongClickJob()
            } else {
                decrement()
            }
        }

        binding.plusButton.setOnClickListener {
            if (longClickRunning()) {
                stopLongClickJob()
            } else {
                increment()
            }
        }

        binding.plusButton.setOnLongClickListener {
            startLongClickJob { increment() }
            return@setOnLongClickListener true
        }

        binding.minusButton.setOnLongClickListener {
            startLongClickJob { decrement() }
            return@setOnLongClickListener true
        }

        binding.plusButton.setOnTouchListener(TouchListener())

        binding.minusButton.setOnTouchListener(TouchListener())
    }

    private fun startLongClickJob(action: ()->Unit) {
        longClickJob = CoroutineScope(Dispatchers.Main).launch {
            var timeout = 800L
            while (true) {
                delay(timeout)
                action.invoke()
                if (timeout > 100) timeout -= 150
            }
        }
    }

    private fun longClickRunning() = longClickJob?.isActive == true

    private fun stopLongClickJob() {
        Timber.d("stopLongClickJob")
        if (longClickRunning()) longClickJob?.cancel()
    }

    fun setOnValueChangedListener(listener: (Int)->Unit) {
        valueChangedListener = listener
    }

    init {
        Timber.d("BasePlusMinusEdit init")
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewNumberPickerBinding.inflate(inflater, this)
        setupUI()
    }

    inner class TouchListener: OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            Timber.d("onTouch ${event?.action}")
            v?.onTouchEvent(event)
            if (event?.action == MotionEvent.ACTION_UP && longClickRunning()) {
                stopLongClickJob()
            }
            return true
        }
    }
}