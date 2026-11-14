package com.ruuvi.station.network.domain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class HttpLoggingTest {
    private val lines = CopyOnWriteArrayList<String>()
    private val logger = HttpLoggingInterceptor.Logger { lines.add(it) }

    // the logging interceptor runs first; the second one answers, so nothing is sent to the network
    private fun call(debug: Boolean) {
        val client = OkHttpClient.Builder()
            .addInterceptor(createHttpLoggingInterceptor(debug, logger))
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(BODY.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val request = Request.Builder()
            .url("https://example.invalid/verify?token=$CODE&email=user%40example.com")
            .header("Authorization", "Bearer $SESSION_TOKEN")
            .build()
        client.newCall(request).execute().close()
    }

    @Test
    fun `release builds log nothing`() {
        call(debug = false)

        assertTrue(lines.isEmpty())
    }

    @Test
    fun `debug builds hide the session token and the code and e-mail in the URL`() {
        call(debug = true)

        val log = lines.joinToString("\n")
        assertTrue(log.contains("/verify?"))
        assertTrue(log.contains("\"result\":\"success\""))
        assertFalse(log.contains(SESSION_TOKEN))
        assertFalse(log.contains(CODE))
        assertFalse(log.contains("user%40example.com"))
        assertFalse(log.contains("user@example.com"))
    }

    private companion object {
        const val CODE = "QZ7K"
        const val SESSION_TOKEN = "session-token-value"
        const val BODY = """{"result":"success","data":{"accessToken": "$SESSION_TOKEN"}}"""
    }
}
