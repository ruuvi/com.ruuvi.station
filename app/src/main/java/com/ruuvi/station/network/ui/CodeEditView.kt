package com.ruuvi.station.network.ui

import android.content.Context
import android.text.Editable
import android.text.InputType.*
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.ruuvi.station.databinding.ViewCodeEditBinding
import timber.log.Timber
import java.util.*

typealias CodeEnteredHandler = (String) -> Unit

class CodeEditView @JvmOverloads
    constructor(
        private val ctx: Context,
        private val attributeSet: AttributeSet? = null,
        private val defStyleAttr: Int = 0
): LinearLayout(ctx, attributeSet, defStyleAttr) {
    var binding: ViewCodeEditBinding

    init {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewCodeEditBinding.inflate(inflater, this)
        setupUI()
    }

    var onCodeEntered: CodeEnteredHandler? = null

    private fun setupUI() {
        with(binding) {
            code1EditText.addTextChangedListener(CodeEditTextWatcher(code2EditText))
            code2EditText.addTextChangedListener(CodeEditTextWatcher(code3EditText))
            code3EditText.addTextChangedListener(CodeEditTextWatcher(code4EditText))
            code4EditText.addTextChangedListener(CodeEditTextWatcher(null))

            code2EditText.keyListener = CodeEditKeyListener(code1EditText)
            code3EditText.keyListener = CodeEditKeyListener(code2EditText)
            code4EditText.keyListener = CodeEditKeyListener(code3EditText)
        }
    }

    fun handlePaste(input: String) {
        if (input.length >= 4) {
            binding.code1EditText.setText(input[0].toString())
            binding.code2EditText.setText(input[1].toString())
            binding.code3EditText.setText(input[2].toString())
            binding.code4EditText.setText(input[3].toString())
        }
    }

    fun clear() {
        binding.code1EditText.setText("")
        binding.code2EditText.setText("")
        binding.code3EditText.setText("")
        binding.code4EditText.setText("")
        binding.code1EditText.requestFocus()
    }

    private fun processChanges() {
        if (onCodeEntered == null ||
            binding.code1EditText.text.isEmpty() ||
            binding.code2EditText.text.isEmpty() ||
            binding.code3EditText.text.isEmpty() ||
            binding.code4EditText.text.isEmpty()) return

        val code = binding.code1EditText.text.toString() +
                binding.code2EditText.text.toString() +
                binding.code3EditText.text.toString() +
                binding.code4EditText.text.toString()
        Timber.d("processChanges $code")
        if (code.length == 4) {
            onCodeEntered?.invoke(code)
        }
    }

    inner class CodeEditTextWatcher(private val nextControl: EditText?) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            Timber.d("onTextChanged $s $start $before $count")
            if (count > 0 && nextControl != null) nextControl.requestFocus()
        }

        override fun afterTextChanged(s: Editable) {
            Timber.d("afterTextChanged $s")

            val input = s.toString().trim().uppercase(Locale.getDefault())
            when {
                input.length >= 4 -> {
                    handlePaste(input)
                }
                input.length > 1 -> {
                    s.clear()
                    s.append(input.last())
                }
                else -> {
                    processChanges()
                }
            }
        }
    }

    inner class CodeEditKeyListener(private val nextControl: EditText) : KeyListener {
        override fun getInputType(): Int {
            return TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_CAP_CHARACTERS
        }

        override fun onKeyDown(
            view: View?,
            text: Editable?,
            keyCode: Int,
            event: KeyEvent?
        ): Boolean {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                nextControl.text.clear()
                nextControl.requestFocus()
            }
            return false
        }

        override fun onKeyUp(
            view: View?,
            text: Editable?,
            keyCode: Int,
            event: KeyEvent?
        ): Boolean {
            return false
        }

        override fun onKeyOther(view: View?, text: Editable?, event: KeyEvent?): Boolean {
            return false
        }

        override fun clearMetaKeyState(view: View?, content: Editable?, states: Int) {}
    }
}