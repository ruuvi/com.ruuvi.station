package com.ruuvi.station.settings.ui

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlertNotificationsSettingsViewModelTest {

    private lateinit var interactor: AppSettingsInteractor
    private lateinit var viewModel: AlertNotificationsSettingsViewModel

    @Before
    fun setUp() {
        interactor = mockk(relaxed = true)
        every { interactor.isLimitLocalAlertsEnabled() } returns false
        every { interactor.isEmailAlerts() } returns false
        every { interactor.isPushAlerts() } returns false

        viewModel = AlertNotificationsSettingsViewModel(interactor)
    }

    @Test
    fun `initial state flow values match interactor defaults`() {
        assertFalse(viewModel.limitLocalAlerts.value)
        assertFalse(viewModel.emailAlerts.value)
        assertFalse(viewModel.pushAlerts.value)
    }

    @Test
    fun `setLimitLocalAlerts updates state flow and interactor`() {
        every { interactor.isLimitLocalAlertsEnabled() } returns true
        viewModel.setLimitLocalAlerts(true)

        verify { interactor.setLimitLocalAlertsEnabled(true) }
        assertTrue(viewModel.limitLocalAlerts.value)
    }

    @Test
    fun `setEmailAlerts updates state flow and interactor`() {
        every { interactor.isEmailAlerts() } returns true
        viewModel.setEmailAlerts(true)

        verify { interactor.setEmailAlerts(true) }
        assertTrue(viewModel.emailAlerts.value)
    }

    @Test
    fun `setPushAlerts updates state flow and interactor`() {
        every { interactor.isPushAlerts() } returns true
        viewModel.setPushAlerts(true)

        verify { interactor.setPushAlerts(true) }
        assertTrue(viewModel.pushAlerts.value)
    }
}
