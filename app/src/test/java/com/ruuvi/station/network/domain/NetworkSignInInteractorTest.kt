package com.ruuvi.station.network.domain

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.firebase.domain.PushRegisterInteractor
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.UserVerifyResponse
import com.ruuvi.station.network.data.response.UserVerifyResponseBody
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkSignInInteractorTest {
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val pushRegisterInteractor = mockk<PushRegisterInteractor>(relaxed = true)
    private val interactor = NetworkSignInInteractor(
        tagRepository = mockk(relaxed = true),
        networkInteractor = networkInteractor,
        networkDataSyncInteractor = mockk(relaxed = true),
        networkTokenRepository = mockk(relaxed = true),
        networkRequestRepository = mockk(relaxed = true),
        sensorSettingsRepository = mockk(relaxed = true),
        pushRegisterInteractor = pushRegisterInteractor,
        preferencesRepository = mockk(relaxed = true),
    )

    @Test
    fun `successful response with data signs in`() {
        assertEquals(listOf<UiText?>(null), signIn(successResponse()))

        verify(exactly = 1) { networkInteractor.verifyUser(CODE, EMAIL, any()) }
        verify(exactly = 1) { pushRegisterInteractor.checkAndRegisterDeviceToken() }
    }

    @Test
    fun `body without result is not a sign in`() {
        val response: UserVerifyResponse = Gson().fromJson(
            """{"data":{"email":"$EMAIL","accessToken":"token","newUser":false}}""",
            object : TypeToken<UserVerifyResponse>() {}.type
        )

        assertEquals(listOf<UiText?>(UNKNOWN_ERROR), signIn(response))

        verify(exactly = 0) { pushRegisterInteractor.checkAndRegisterDeviceToken() }
    }

    @Test
    fun `successful response without data is not a sign in`() {
        assertEquals(listOf<UiText?>(UNKNOWN_ERROR), signIn(successResponse().copy(data = null)))

        verify(exactly = 0) { pushRegisterInteractor.checkAndRegisterDeviceToken() }
    }

    @Test
    fun `missing response shows the unknown error`() {
        assertEquals(listOf<UiText?>(UNKNOWN_ERROR), signIn(null))

        verify(exactly = 0) { pushRegisterInteractor.checkAndRegisterDeviceToken() }
    }

    @Test
    fun `error without a message shows the unknown error`() {
        val response = successResponse().copy(result = RuuviNetworkResponse.errorResult, data = null)

        assertEquals(listOf<UiText?>(UNKNOWN_ERROR), signIn(response))
    }

    @Test
    fun `error message is passed on`() {
        val response = successResponse().copy(
            result = RuuviNetworkResponse.errorResult,
            error = "That code is incorrect.",
            data = null,
            code = NetworkResponseLocalizer.ER_TOKEN_EXPIRED,
            subCode = NetworkResponseLocalizer.ER_SUB_INVALID_CODE,
        )

        assertEquals(listOf<UiText?>(UiText.DynamicString("That code is incorrect.")), signIn(response))

        verify(exactly = 0) { pushRegisterInteractor.checkAndRegisterDeviceToken() }
    }

    private fun signIn(response: UserVerifyResponse?): List<UiText?> {
        val onResult = slot<(UserVerifyResponse?) -> Unit>()
        every { networkInteractor.verifyUser(any(), any(), capture(onResult)) } answers {
            onResult.captured(response)
        }
        val results = mutableListOf<UiText?>()
        interactor.signIn(CODE, EMAIL) { results.add(it) }
        return results
    }

    private fun successResponse(): UserVerifyResponse =
        RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = UserVerifyResponseBody(email = EMAIL, accessToken = "token", newUser = false),
            code = null,
        )

    private companion object {
        const val CODE = "AB12"
        const val EMAIL = "user@example.com"
        val UNKNOWN_ERROR = UiText.StringResource(R.string.unknown_error)
    }
}
