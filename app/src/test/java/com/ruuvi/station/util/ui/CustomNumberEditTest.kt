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
@Config(application = CustomNumberEditTestApplication::class, sdk = [35])
class CustomNumberEditTest {

    private val context: Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.AppTheme
        )

    private lateinit var customNumberEdit: CustomNumberEdit

    @Before
    fun setUp() {
        customNumberEdit = CustomNumberEdit(context)
        customNumberEdit.elements = listOf(
            CustomNumberEdit.SelectionElement(value = 10, resourceArgument = null, captionResource = R.string.ok),
            CustomNumberEdit.SelectionElement(value = 20, resourceArgument = null, captionResource = R.string.cancel),
            CustomNumberEdit.SelectionElement(value = 30, resourceArgument = null, captionResource = R.string.done),
        )
    }

    @Test
    fun `setSelectedItem selects exact value`() {
        var selectedValue = -1
        customNumberEdit.setOnValueChangedListener { value ->
            selectedValue = value
        }

        customNumberEdit.setSelectedItem(20)

        assertEquals(20, selectedValue)
    }

    @Test
    fun `setSelectedItem selects closest value when exact value absent`() {
        var selectedValue = -1
        customNumberEdit.setOnValueChangedListener { value ->
            selectedValue = value
        }

        customNumberEdit.setSelectedItem(24) // 24 is closer to 20 than 30

        assertEquals(20, selectedValue)
    }

    @Test
    fun `increment moves to next selection element`() {
        var selectedValue = -1
        customNumberEdit.setOnValueChangedListener { value ->
            selectedValue = value
        }

        customNumberEdit.setSelectedItem(10)
        customNumberEdit.increment()

        assertEquals(20, selectedValue)
    }

    @Test
    fun `decrement moves to previous selection element`() {
        var selectedValue = -1
        customNumberEdit.setOnValueChangedListener { value ->
            selectedValue = value
        }

        customNumberEdit.setSelectedItem(30)
        customNumberEdit.decrement()

        assertEquals(20, selectedValue)
    }

    @Test
    fun `increment does not exceed bounds`() {
        var selectedValue = -1
        customNumberEdit.setOnValueChangedListener { value ->
            selectedValue = value
        }

        customNumberEdit.setSelectedItem(30)
        customNumberEdit.increment()

        assertEquals(30, selectedValue)
    }

    @Test
    fun `decrement does not go below index zero`() {
        var selectedValue = -1
        customNumberEdit.setOnValueChangedListener { value ->
            selectedValue = value
        }

        customNumberEdit.setSelectedItem(10)
        customNumberEdit.decrement()

        assertEquals(10, selectedValue)
    }
}

class CustomNumberEditTestApplication : Application()
