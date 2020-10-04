package com.ruuvi.station.network

import com.ruuvi.station.network.data.UserRegisterRequest
import com.ruuvi.station.network.data.UserVerifyRequest
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import org.junit.Test

class UserApiTests {

    @Test
    fun TestRegisterUser() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        val user = UserRegisterRequest(1, "denis@ruuvi.com")
        networkRepository.registerUser(user) {
            if (it != null) {
                println("response: ")
                println(it)
            } else {
                println("empty response")
            }
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestVerifyUser() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MDE2NDM2NDksImlhdCI6MTYwMTY0Mjc0OSwiZGF0YSI6eyJlbWFpbCI6ImFuZHJlZXYuZGVuaXNAZ21haWwuY29tIiwidHlwZSI6InJlZ2lzdHJhdGlvbiJ9fQ.YnfNF44f5RD3aq__12_4sUZPYabmWKxzgSzRxkwMmms"
        networkRepository.verifyUser(token) {
            if (it != null) {
                println("response: ")
                println(it)
            } else {
                println("empty response")
            }
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestGetUserData() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        val token = "rKJI8hFZqO9AdI2YX9mbGXnOJrs6jDldUAcLf2AWK59LAYQCXvhQOqxkroEvUIIP"
        networkRepository.getUserInfo(token) {
            if (it != null) {
                println("response: ")
                println(it)
            } else {
                println("empty response")
            }
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }
}
