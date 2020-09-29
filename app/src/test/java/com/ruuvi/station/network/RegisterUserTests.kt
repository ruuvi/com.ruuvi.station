package com.ruuvi.station.network

import com.ruuvi.station.network.data.UserRegisterRequest
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
}
