package com.ruuvi.station.network

import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ShareSensorRequest
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import org.junit.Test

class UserApiTests {

    @Test
    fun TestRegisterUser() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        val user = UserRegisterRequest("andreev.denis@gmail.com")
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

        val token = "UF7K2U"
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

        val token = "7531/L6gN2uwABFfsJI8R0tc6BPHCjnscxlxx7sjed2pEO4csHj8RPoLdsWZo9CrGtyHJ"
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

    @Test
    fun TestClaimTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        val token = "7531/L6gN2uwABFfsJI8R0tc6BPHCjnscxlxx7sjed2pEO4csHj8RPoLdsWZo9CrGtyHJ"
        networkRepository.claimSensor(ClaimSensorRequest("some tag2", "D0:FA:46:09:9E:7A"), token) {
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
    fun TestShareTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()
        val token = "7531/L6gN2uwABFfsJI8R0tc6BPHCjnscxlxx7sjed2pEO4csHj8RPoLdsWZo9CrGtyHJ"

        networkRepository.shareSensor(ShareSensorRequest("denis@ruuvi.com", "D0:FA:46:09:9E:7A"), token) {
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
