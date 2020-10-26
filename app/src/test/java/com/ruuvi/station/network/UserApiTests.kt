package com.ruuvi.station.network

import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import com.ruuvi.station.rules.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.*

@ExperimentalCoroutinesApi
class UserApiTests {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    //@Test
    fun TestRegisterUser() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository(Dispatchers.Unconfined)

        networkRepository.registerUser(UserRegisterRequest(sacrificialEmail)) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            Assert.assertEquals(sacrificialEmail, it?.data?.email)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    //@Test
    fun TestRegisterUserError() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository(Dispatchers.Unconfined)

        networkRepository.registerUser(UserRegisterRequest("11111")) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data == null)
            Assert.assertEquals(errorResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    //@Test
    fun TestVerifyUserError() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository(Dispatchers.Unconfined)

        networkRepository.verifyUser(fakeCode) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data == null)
            Assert.assertEquals(errorResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    //@Test
    fun TestGetUserData() {
//        val lock1 = Object()
//        val networkRepository = RuuviNetworkRepository(Dispatchers.Unconfined)
//
//        networkRepository.getUserInfo(accessToken) {
//            Assert.assertTrue(it != null)
//            Assert.assertTrue(it?.data != null)
//            Assert.assertEquals(successResult, it?.result)
//            Assert.assertEquals(sacrificialEmail, it?.data?.email)
//            //todo add data.sensors test here
//            synchronized(lock1) { lock1.notify() }
//        }
//        synchronized(lock1) { lock1.wait() }
    }

    //@Test
    fun TestClaimTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository(Dispatchers.Unconfined)

        //todo add unclaim
        networkRepository.claimSensor(ClaimSensorRequest("some tag2", "D0:D0:D0:D0:D0:D0"), accessToken) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    //@Test
    fun TestUnclaimTag() = runBlocking {
        val networkRepository = RuuviNetworkRepository(coroutineTestRule.dispatcher)

        val response = networkRepository.unclaimSensor(UnclaimSensorRequest("F9:A4:9F:1A:D9:10"), accessToken)
        Assert.assertTrue(response != null)
        Assert.assertEquals(successResult, response?.result)
    }

    //@Test
    fun TestShareTag() {
        val lock1 = Object()
        val networkRepository = RuuviNetworkRepository(Dispatchers.Unconfined)

        //todo add claim, share, unshare, unclaim
        networkRepository.shareSensor(ShareSensorRequest("denis@ruuvi.com", "D0:D0:D0:D0:D0:D0"), accessToken) {
            Assert.assertTrue(it != null)
            Assert.assertTrue(it?.data != null)
            Assert.assertEquals(successResult, it?.result)
            synchronized(lock1) { lock1.notify() }
        }
        synchronized(lock1) { lock1.wait() }
    }

    @ExperimentalCoroutinesApi
    //@Test
    fun TestGetSensorData() = runBlocking{
        val networkRepository = RuuviNetworkRepository(coroutineTestRule.dispatcher)

        val request = GetSensorDataRequest(
            sensor = "C6:02:66:2B:09:C8",
            since = Date(),
            sort = "asc",
            limit = 100
        )

        val result = networkRepository.getSensorData(accessToken, request)

        Assert.assertTrue(result != null)
        Assert.assertTrue(result?.data != null)
        Assert.assertEquals(successResult, result?.result)

    }

    companion object {
        val sacrificialEmail = "mzad1203@gmail.com"
        val fakeCode = "XXXXXX"
        val accessToken = "753131/Sp5D6Geb8w0oZBtkMZcVfjH56i0AuzCvEGkMAPjifZeuDMKwnU1xWxXh9jVZ9tYl"
        val errorResult = "error"
        val successResult = "success"
    }
}
