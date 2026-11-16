package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.response.UserVerifyResponse
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class RuuviNetworkRepositoryTest {
    private val requests = CopyOnWriteArrayList<Request>()

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

    private val repository = RuuviNetworkRepository(
        dispatcher = Dispatchers.IO,
        imageInteractor = mockk(relaxed = true),
        preferencesRepository = mockk(relaxed = true),
        httpClient = client
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verifyUser sends the code as token and the address as email`() {
        val result = CompletableDeferred<UserVerifyResponse?>()

        repository.verifyUser("AB12", "user@example.com") { result.complete(it) }
        val response = runBlocking { withTimeout(TIMEOUT_MS) { result.await() } }

        val url = requests.single().url
        assertEquals("/verify", url.encodedPath)
        assertEquals("token=AB12&email=user%40example.com", url.encodedQuery)
        assertEquals("ER_SUB_INVALID_CODE", response?.subCode)
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val ERROR_BODY =
            """{"result":"error","error":"That code is incorrect.","code":"ER_TOKEN_EXPIRED","sub_code":"ER_SUB_INVALID_CODE"}"""
    }
}
