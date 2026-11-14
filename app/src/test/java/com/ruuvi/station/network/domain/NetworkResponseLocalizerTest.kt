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
    fun `missing or unknown sub code preserves existing error messages`() {
        val messages = listOf(
            NetworkResponseLocalizer.ER_FORBIDDEN to R.string.cloud_er_forbidden,
            NetworkResponseLocalizer.ER_UNAUTHORIZED to R.string.cloud_er_unauthorized,
            NetworkResponseLocalizer.ER_INTERNAL to R.string.cloud_er_internal,
            NetworkResponseLocalizer.ER_INVALID_FORMAT to R.string.cloud_er_invalid_format,
            NetworkResponseLocalizer.ER_USER_NOT_FOUND to R.string.cloud_er_user_not_found,
            NetworkResponseLocalizer.ER_SENSOR_NOT_FOUND to R.string.cloud_er_sensor_not_found,
            NetworkResponseLocalizer.ER_TOKEN_EXPIRED to R.string.cloud_er_token_expired,
            NetworkResponseLocalizer.ER_SUBSCRIPTION_NOT_FOUND to R.string.cloud_er_subscription_not_found,
            NetworkResponseLocalizer.ER_SHARE_COUNT_REACHED to R.string.cloud_er_share_count_reached,
            NetworkResponseLocalizer.ER_SENSOR_SHARE_COUNT_REACHED to R.string.cloud_er_sensor_share_count_reached,
            NetworkResponseLocalizer.ER_NO_DATA_TO_SHARE to R.string.cloud_er_no_data_to_share,
            NetworkResponseLocalizer.ER_SENSOR_ALREADY_SHARED to R.string.cloud_er_sensor_already_shared,
            NetworkResponseLocalizer.ER_UNABLE_TO_SEND_EMAIL to R.string.cloud_er_unable_to_send_email,
            NetworkResponseLocalizer.ER_MISSING_ARGUMENT to R.string.cloud_er_missing_argument,
            NetworkResponseLocalizer.ER_INVALID_DENSITY_MODE to R.string.cloud_er_invalid_density_mode,
            NetworkResponseLocalizer.ER_INVALID_SORT_MODE to R.string.cloud_er_invalid_sort_mode,
            NetworkResponseLocalizer.ER_INVALID_TIME_RANGE to R.string.cloud_er_invalid_time_range,
            NetworkResponseLocalizer.ER_INVALID_EMAIL_ADDRESS to R.string.cloud_er_invalid_email_address,
            NetworkResponseLocalizer.ER_INVALID_MAC_ADDRESS to R.string.cloud_er_invalid_mac_address,
            NetworkResponseLocalizer.ER_SUB_DATA_STORAGE_ERROR to R.string.cloud_er_sub_data_storage_error,
            NetworkResponseLocalizer.ER_SUB_NO_USER to R.string.cloud_er_sub_no_user,
        )

        for ((code, message) in messages) {
            for (subCode in listOf(null, "ER_SUB_SOMETHING_NEW")) {
                assertEquals("code=$code, subCode=$subCode", context.getString(message), localize(code, subCode))
            }
        }
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
