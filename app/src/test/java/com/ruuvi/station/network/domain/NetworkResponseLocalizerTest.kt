package com.ruuvi.station.network.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.UserVerifyResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NetworkResponseLocalizerTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val localizer = NetworkResponseLocalizer(context)

    @Test
    fun `expired token without a sub code keeps the old message`() {
        assertEquals(
            context.getString(R.string.cloud_er_token_expired),
            localize(NetworkResponseLocalizer.ER_TOKEN_EXPIRED)
        )
    }

    @Test
    fun `invalid code sub code has its own message`() {
        assertEquals(
            context.getString(R.string.cloud_er_sub_invalid_code),
            localize(NetworkResponseLocalizer.ER_TOKEN_EXPIRED, NetworkResponseLocalizer.ER_SUB_INVALID_CODE)
        )
    }

    @Test
    fun `used code sub code has its own message`() {
        assertEquals(
            context.getString(R.string.cloud_er_sub_code_used),
            localize(NetworkResponseLocalizer.ER_TOKEN_EXPIRED, NetworkResponseLocalizer.ER_SUB_CODE_USED)
        )
    }

    @Test
    fun `expired code sub code has its own message`() {
        assertEquals(
            context.getString(R.string.cloud_er_sub_code_expired),
            localize(NetworkResponseLocalizer.ER_TOKEN_EXPIRED, NetworkResponseLocalizer.ER_SUB_CODE_EXPIRED)
        )
    }

    @Test
    fun `too many attempts sub code has its own message`() {
        assertEquals(
            context.getString(R.string.cloud_er_sub_too_many_attempts),
            localize(ER_THROTTLED, NetworkResponseLocalizer.ER_SUB_TOO_MANY_ATTEMPTS)
        )
    }

    @Test
    fun `sub code messages differ from each other and from the old message`() {
        val messages = listOf(
            R.string.cloud_er_token_expired,
            R.string.cloud_er_sub_invalid_code,
            R.string.cloud_er_sub_code_used,
            R.string.cloud_er_sub_code_expired,
            R.string.cloud_er_sub_too_many_attempts,
        ).map { context.getString(it) }

        assertEquals(messages.size, messages.toSet().size)
    }

    @Test
    fun `unknown sub code falls back to the code`() {
        assertEquals(
            context.getString(R.string.cloud_er_token_expired),
            localize(NetworkResponseLocalizer.ER_TOKEN_EXPIRED, "ER_SUB_SOMETHING_NEW")
        )
    }

    @Test
    fun `no user sub code is still resolved from the code`() {
        assertEquals(
            context.getString(R.string.cloud_er_internal),
            localize(NetworkResponseLocalizer.ER_INTERNAL, NetworkResponseLocalizer.ER_SUB_NO_USER)
        )
    }

    @Test
    fun `throttled without a sub code keeps the server text`() {
        assertEquals(SERVER_TEXT, localize(ER_THROTTLED))
    }

    @Test
    fun `unknown code keeps the server text`() {
        assertEquals(SERVER_TEXT, localize("ER_SOMETHING_NEW", "ER_SUB_SOMETHING_NEW"))
        assertEquals(SERVER_TEXT, localize(null))
    }

    @Test
    fun `response that is not an error is left untouched`() {
        val response = errorResponse(
            NetworkResponseLocalizer.ER_TOKEN_EXPIRED,
            NetworkResponseLocalizer.ER_SUB_INVALID_CODE
        ).copy(result = RuuviNetworkResponse.successResult)

        localizer.localizeResponse(response)

        assertEquals(SERVER_TEXT, response.error)
    }

    private fun localize(code: String?, subCode: String? = null): String {
        val response = errorResponse(code, subCode)
        localizer.localizeResponse(response)
        return response.error
    }

    private fun errorResponse(code: String?, subCode: String?): UserVerifyResponse =
        RuuviNetworkResponse(
            result = RuuviNetworkResponse.errorResult,
            error = SERVER_TEXT,
            data = null,
            code = code,
            subCode = subCode,
        )

    private companion object {
        const val SERVER_TEXT = "Text from the server"
        const val ER_THROTTLED = "ER_THROTTLED"
    }
}
