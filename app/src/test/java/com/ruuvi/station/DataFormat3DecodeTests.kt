package com.ruuvi.station

import com.ruuvi.station.bluetooth.decoder.DecodeFormat3
import com.ruuvi.station.util.extensions.hexStringToByteArray
import org.junit.Assert

import org.junit.Test

class DataFormat3DecodeTests {
    val dataFormat3 = DecodeFormat3()

    @Test
    fun testDecode3PlusTemp() {
        val result = dataFormat3.decode(getTestDataPlusTemperature(), 7)
        Assert.assertNotNull(result?.temperature)
        Assert.assertTrue(result?.temperature!! > 0f)
    }

    @Test
    fun testDecode3MinusTemp() {
        val result = dataFormat3.decode(getTestDataMinusTemperature(), 7)
        Assert.assertNotNull(result?.temperature)
        Assert.assertTrue(result?.temperature!! < 0f)
    }

    @Test
    fun testDecode3ValidData(){
        val inputData = "03291A1ECE1EFC18F94202CA0B53".hexStringToByteArray()
        val result = dataFormat3.decode(inputData, 0)
        Assert.assertEquals(3, result?.dataFormat)
        Assert.assertEquals(26.3, result?.temperature)
        Assert.assertEquals(102766.0, result?.pressure)
        Assert.assertEquals(20.5, result?.humidity)
        Assert.assertEquals(-1.0, result?.accelX)
        Assert.assertEquals(-1.726, result?.accelY)
        Assert.assertEquals(0.714, result?.accelZ)
        Assert.assertEquals(2.899, result?.voltage)
    }

    @Test
    fun testDecode3MaximumValues(){
        val inputData = "03FF7F63FFFF7FFF7FFF7FFFFFFF".hexStringToByteArray()
        val result = dataFormat3.decode(inputData, 0)
        Assert.assertEquals(3, result?.dataFormat)
        Assert.assertEquals(127.99, result?.temperature)
        Assert.assertEquals(115535.0, result?.pressure)
        Assert.assertEquals(127.5, result?.humidity)
        Assert.assertEquals(32.767, result?.accelX)
        Assert.assertEquals(32.767, result?.accelY)
        Assert.assertEquals(32.767, result?.accelZ)
        Assert.assertEquals(65.535, result?.voltage)
    }

    @Test
    fun testDecode3MinimumValues(){
        val inputData = "0300FF6300008001800180010000".hexStringToByteArray()
        val result = dataFormat3.decode(inputData, 0)
        Assert.assertEquals(3, result?.dataFormat)
        Assert.assertEquals(-127.99, result?.temperature)
        Assert.assertEquals(50000.0, result?.pressure)
        Assert.assertEquals(0.0, result?.humidity)
        Assert.assertEquals(-32.767, result?.accelX)
        Assert.assertEquals(-32.767, result?.accelY)
        Assert.assertEquals(-32.767, result?.accelZ)
        Assert.assertEquals(0.0, result?.voltage)
   }

    fun getTestDataMinusTemperature(): ByteArray {
        val result = ByteArray(25)
        result[0] = 2
        result[1] = 1
        result[2] = 6
        result[3] = 17
        result[4] = -1
        result[5] = -103
        result[6] = 4
        result[7] = 3
        result[8] = 77
        result[9] = -118
        result[10] = 17
        result[11] = -60
        result[12] = 17
        result[13] = 0
        result[14] = -5
        result[15] = -1
        result[16] = -26
        result[17] = 3
        result[18] = -21
        result[19] = 12
        result[20] = 37
        result[21] = 0
        result[22] = 0
        result[23] = 0
        result[24] = 0
        return result
    }

    fun getTestDataPlusTemperature(): ByteArray {
        val result = ByteArray(25)
        result[0] = 2
        result[1] = 1
        result[2] = 6
        result[3] = 17
        result[4] = -1
        result[5] = -103
        result[6] = 4
        result[7] = 3
        result[8] = 83
        result[9] = 27
        result[10] = 17
        result[11] = -60
        result[12] = 43
        result[13] = 0
        result[14] = 41
        result[15] = 0
        result[16] = 13
        result[17] = 4
        result[18] = 6
        result[19] = 12
        result[20] = 67
        result[21] = 0
        result[22] = 0
        result[23] = 0
        result[24] = 0
        return result
    }
}