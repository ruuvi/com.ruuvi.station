package com.ruuvi.station.network.domain

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RuuviNetworkApiTest {
    private val requests = mutableListOf<Request>()

    // answers every request itself, nothing is sent to the network
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body(ERROR_BODY.toResponseBody("application/json".toMediaType()))
                .build()
        }
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(RuuviNetworkApi::class.java)

    @Test
    fun `verifyUser sends the code and the e-mail as query parameters`() {
        val response = runBlocking { api.verifyUser("AB12", "first.last+tag@example.com") }

        assertFalse(response.isSuccessful)
        val url = requests.single().url
        assertEquals("GET", requests.single().method)
        assertEquals("/verify", url.encodedPath)
        assertEquals(setOf("token", "email"), url.queryParameterNames)
        assertEquals("AB12", url.queryParameter("token"))
        assertEquals("first.last+tag@example.com", url.queryParameter("email"))
        // a bare + would be read as a space by the server
        assertTrue(url.encodedQuery.orEmpty().contains("first.last%2Btag"))
    }

    private companion object {
        const val ERROR_BODY =
            """{"result":"error","error":"That code is incorrect.","code":"ER_TOKEN_EXPIRED","sub_code":"ER_SUB_INVALID_CODE"}"""
    }
}
