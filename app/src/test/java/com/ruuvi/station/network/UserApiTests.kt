package com.ruuvi.station.network

import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ShareSensorRequest
import com.ruuvi.station.network.data.request.UnclaimSensorRequest
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import org.junit.Assert
import org.junit.Test

class UserApiTests {
    @Test
    fun TestRegisterUser() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        networkRepository.registerUser(UserRegisterRequest(sacrificialEmail)) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            Assert.assertEquals(sacrificialEmail, it?.data?.email)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestRegisterUserError() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        networkRepository.registerUser(UserRegisterRequest("11111")) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data == null)
            Assert.assertEquals(errorResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestVerifyUserError() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        networkRepository.verifyUser(fakeCode) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data == null)
            Assert.assertEquals(errorResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestGetUserData() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        networkRepository.getUserInfo(accessToken) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            Assert.assertEquals(sacrificialEmail, it?.data?.email)
            //todo add data.sensors test here
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestClaimTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        //todo add unclaim
        networkRepository.claimSensor(ClaimSensorRequest("some tag2", "D0:D0:D0:D0:D0:D0"), accessToken) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestUnclaimTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        //todo add unclaim
        networkRepository.unclaimSensor(UnclaimSensorRequest("D6:78:01:D8:86:99"), accessToken) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestShareTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        //todo add claim, share, unshare, unclaim
        networkRepository.shareSensor(ShareSensorRequest("denis@ruuvi.com", "D0:D0:D0:D0:D0:D0"), accessToken) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @Test
    fun TestGetSensorData() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository()

        networkRepository.getSensorData(accessToken, "ea:ec:a6:1c:fc:9d") {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            val sens = it?.data?.measurements?.first()
            val tag = BluetoothLibrary.decode(sens!!.sensor, sens!!.data, sens!!.rssi)
            println(it.toString())
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    companion object {
        val sacrificialEmail = "mzad1203@gmail.com"
        val fakeCode = "XXXXXX"
        val accessToken = "753131/Sp5D6Geb8w0oZBtkMZcVfjH56i0AuzCvEGkMAPjifZeuDMKwnU1xWxXh9jVZ9tYl"
        val errorResult = "error"
        val successResult = "success"
    }
}
