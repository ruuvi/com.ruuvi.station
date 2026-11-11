package com.ruuvi.station.util.ui

import android.app.Application
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SequenceNumberEditTestApplication::class, sdk = [35])
class SequenceNumberEditTest {

    private val context: Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.AppTheme
        )

    private lateinit var sequenceNumberEdit: SequenceNumberEdit

    @Before
    fun setUp() {
        sequenceNumberEdit = SequenceNumberEdit(context)
    }

    @Test
    fun `selectedValue updates value and notifies listener`() {
        var listenerValue = -1
        sequenceNumberEdit.setOnValueChangedListener { value ->
            listenerValue = value
        }

        sequenceNumberEdit.selectedValue = 15

        assertEquals(15, sequenceNumberEdit.selectedValue)
        assertEquals(15, listenerValue)
    }

    @Test
    fun `selectedValue clamps value below minimum`() {
        sequenceNumberEdit.selectedValue = -10

        assertEquals(SequenceNumberEdit.MIN_VALUE_DEFAULT, sequenceNumberEdit.selectedValue)
    }

    @Test
    fun `selectedValue clamps value above maximum`() {
        sequenceNumberEdit.selectedValue = 100

        assertEquals(SequenceNumberEdit.MAX_VALUE_DEFAULT, sequenceNumberEdit.selectedValue)
    }

    @Test
    fun `increment increases selectedValue by one`() {
        sequenceNumberEdit.selectedValue = 5
        sequenceNumberEdit.increment()

        assertEquals(6, sequenceNumberEdit.selectedValue)
    }

    @Test
    fun `decrement decreases selectedValue by one`() {
        sequenceNumberEdit.selectedValue = 5
        sequenceNumberEdit.decrement()

        assertEquals(4, sequenceNumberEdit.selectedValue)
    }
}

class SequenceNumberEditTestApplication : Application()
