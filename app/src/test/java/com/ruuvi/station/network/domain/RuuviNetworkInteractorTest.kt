package com.ruuvi.station.network.domain

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.UserVerifyResponse
import com.ruuvi.station.network.data.response.UserVerifyResponseBody
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class RuuviNetworkInteractorTest {
    private val tokenRepository = mockk<NetworkTokenRepository>(relaxed = true)
    private val networkRepository = mockk<RuuviNetworkRepository>(relaxed = true)
    private val firebaseInteractor = mockk<FirebaseInteractor>(relaxed = true)
    private val networkResponseLocalizer = mockk<NetworkResponseLocalizer>(relaxed = true)
    private val interactor = RuuviNetworkInteractor(
        tokenRepository = tokenRepository,
        networkRepository = networkRepository,
        networkRequestExecutor = mockk(relaxed = true),
        sensorSettingsRepository = mockk(relaxed = true),
        firebaseInteractor = firebaseInteractor,
        networkResponseLocalizer = networkResponseLocalizer,
    )

    @Test
    fun `verifyUser sends the code with the e-mail and saves the token on success`() {
        val response = successResponse()

        assertEquals(listOf<UserVerifyResponse?>(response), verifyUser(response))

        verify(exactly = 1) { networkRepository.verifyUser(CODE, EMAIL, any()) }
        verify(exactly = 1) { networkResponseLocalizer.localizeResponse(response) }
        verify(exactly = 1) { tokenRepository.saveTokenInfo(NetworkTokenInfo(EMAIL, TOKEN)) }
        verify(exactly = 1) { firebaseInteractor.logSignIn() }
    }

    @Test
    fun `verifyUser does not save a token from a body without result`() {
        val response: UserVerifyResponse = Gson().fromJson(
            """{"data":{"email":"$EMAIL","accessToken":"$TOKEN","newUser":false}}""",
            object : TypeToken<UserVerifyResponse>() {}.type
        )

        assertEquals(listOf<UserVerifyResponse?>(response), verifyUser(response))

        assertNotSignedIn()
    }

    @Test
    fun `verifyUser does not save a token from an error response`() {
        verifyUser(successResponse().copy(result = RuuviNetworkResponse.errorResult))
        assertNotSignedIn()

        verifyUser(successResponse().copy(data = null))
        assertNotSignedIn()

        assertEquals(listOf<UserVerifyResponse?>(null), verifyUser(null))
        assertNotSignedIn()
    }

    private fun assertNotSignedIn() {
        verify(exactly = 0) { tokenRepository.saveTokenInfo(any()) }
        verify(exactly = 0) { firebaseInteractor.logSignIn() }
    }

    private fun verifyUser(response: UserVerifyResponse?): List<UserVerifyResponse?> {
        val onResult = slot<(UserVerifyResponse?) -> Unit>()
        every { networkRepository.verifyUser(any(), any(), capture(onResult)) } answers {
            onResult.captured(response)
        }
        val results = mutableListOf<UserVerifyResponse?>()
        interactor.verifyUser(CODE, EMAIL) { results.add(it) }
        return results
    }

    private fun successResponse(): UserVerifyResponse =
        RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = UserVerifyResponseBody(email = EMAIL, accessToken = TOKEN, newUser = false),
            code = null,
        )

    private companion object {
        const val CODE = "AB12"
        const val EMAIL = "user@example.com"
        const val TOKEN = "token"
    }
}
