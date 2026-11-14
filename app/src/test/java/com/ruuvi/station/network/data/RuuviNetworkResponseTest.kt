package com.ruuvi.station.network.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.network.data.response.UserVerifyResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuuviNetworkResponseTest {
    @Test
    fun `sub_code is parsed from an error body`() {
        val response = parse(
            """{"result":"error","error":"That code is incorrect.","code":"ER_TOKEN_EXPIRED","sub_code":"ER_SUB_INVALID_CODE"}"""
        )

        assertTrue(response.isError())
        assertEquals("ER_TOKEN_EXPIRED", response.code)
        assertEquals("ER_SUB_INVALID_CODE", response.subCode)
        assertEquals("That code is incorrect.", response.error)
        assertNull(response.data)
    }

    @Test
    fun `error body from an old server has no sub code`() {
        val response = parse("""{"result":"error","error":"Token expired.","code":"ER_TOKEN_EXPIRED"}""")

        assertTrue(response.isError())
        assertEquals("ER_TOKEN_EXPIRED", response.code)
        assertNull(response.subCode)
    }

    @Test
    fun `unknown code and sub_code are kept as they are`() {
        val response = parse(
            """{"result":"error","error":"New error.","code":"ER_SOMETHING_NEW","sub_code":"ER_SUB_SOMETHING_NEW","extra":1}"""
        )

        assertEquals("ER_SOMETHING_NEW", response.code)
        assertEquals("ER_SUB_SOMETHING_NEW", response.subCode)
    }

    @Test
    fun `success body is parsed without a sub code`() {
        val response = parse(
            """{"result":"success","data":{"email":"user@example.com","accessToken":"token","newUser":false}}"""
        )

        assertTrue(response.isSuccess())
        assertEquals("user@example.com", response.data?.email)
        assertEquals("token", response.data?.accessToken)
        assertNull(response.subCode)
    }

    @Test
    fun `body without result is neither a success nor an error`() {
        val response = parse("""{"data":{"email":"user@example.com","accessToken":"token","newUser":false}}""")

        assertNotNull(response.data)
        assertFalse(response.isSuccess())
        assertFalse(response.isError())
    }

    // same parsing as RuuviNetworkRepository uses for error bodies
    private fun parse(json: String): UserVerifyResponse =
        Gson().fromJson(json, object : TypeToken<UserVerifyResponse>() {}.type)
}
