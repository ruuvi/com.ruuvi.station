package com.ruuvi.station.network

import com.ruuvi.station.network.data.UserRegisterRequest
import com.ruuvi.station.network.data.UserVerifyRequest
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import org.junit.Test

class RegisterUserTests {

    @Test
    fun TestRegisterUser() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        val user = UserRegisterRequest(0, "andreev.denis@gmail.com")
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

        val token = UserVerifyRequest("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MDE0MTQ0NTQsImlhdCI6MTYwMTQxMzU1NCwiZGF0YSI6eyJlbWFpbCI6ImFuZHJlZXYuZGVuaXNAZ21haWwuY29tIiwidHlwZSI6InJlZ2lzdHJhdGlvbiJ9fQ.ckbfBPMT9qQc4VKTz6_WCkkXzHHX2G8QZ1RcuDgDIk8")
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
}
