package com.ruuvi.station

import com.ruuvi.station.bluetooth.decoder.DecodeFormat5
import com.ruuvi.station.util.extensions.hexStringToByteArray
import org.junit.Assert
import org.junit.Test

class DataFormat5DecodeTests {
    val dataFormat5 = DecodeFormat5()

    @Test
    fun testDecode5ValidData(){
        val inputData = "0512FC5394C37C0004FFFC040CAC364200CDCBB8334C884F".hexStringToByteArray()
        val result = dataFormat5.decode(inputData, 0)
        Assert.assertEquals(5, result?.dataFormat)
        Assert.assertEquals(24.3, result?.temperature)
        Assert.assertEquals(100044.0, result?.pressure)
        Assert.assertEquals(53.49, result?.humidity)
        Assert.assertEquals(0.004, result?.accelX)
        Assert.assertEquals(-0.004, result?.accelY)
        Assert.assertEquals(1.036, result?.accelZ)
        Assert.assertEquals(4.0, result?.txPower)
        Assert.assertEquals(2.977, result?.voltage)
        Assert.assertEquals(66, result?.movementCounter)
        Assert.assertEquals(205, result?.measurementSequenceNumber)
    }

    @Test
    fun testDecode5MaximumValues(){
        val inputData = "057FFFFFFEFFFE7FFF7FFF7FFFFFDEFEFFFECBB8334C884F".hexStringToByteArray()
        val result = dataFormat5.decode(inputData, 0)
        Assert.assertEquals(5, result?.dataFormat)
        Assert.assertEquals(163.835, result?.temperature)
        Assert.assertEquals(115534.0, result?.pressure)
        Assert.assertEquals(163.8350, result?.humidity)
        Assert.assertEquals(32.767, result?.accelX)
        Assert.assertEquals(32.767, result?.accelY)
        Assert.assertEquals(32.767, result?.accelZ)
        Assert.assertEquals(20.0, result?.txPower)
        Assert.assertEquals(3.646, result?.voltage)
        Assert.assertEquals(254, result?.movementCounter)
        Assert.assertEquals(65534, result?.measurementSequenceNumber)
    }

    @Test
    fun testDecode5MinimumValues(){
        val inputData = "058001000000008001800180010000000000CBB8334C884F".hexStringToByteArray()
        val result = dataFormat5.decode(inputData, 0)
        Assert.assertEquals(5, result?.dataFormat)
        Assert.assertEquals(-163.835, result?.temperature)
        Assert.assertEquals(50000.0, result?.pressure)
        Assert.assertEquals(0.000, result?.humidity)
        Assert.assertEquals(-32.767, result?.accelX)
        Assert.assertEquals(-32.767, result?.accelY)
        Assert.assertEquals(-32.767, result?.accelZ)
        Assert.assertEquals(-40.0, result?.txPower)
        Assert.assertEquals(1.600, result?.voltage)
        Assert.assertEquals(0, result?.movementCounter)
        Assert.assertEquals(0, result?.measurementSequenceNumber)
    }

    @Test
    fun testDecode5InvalidValues(){
        val inputData = "058000FFFFFFFF800080008000FFFFFFFFFFFFFFFFFFFFFF".hexStringToByteArray()
        val result = dataFormat5.decode(inputData, 0)
        Assert.assertEquals(5, result?.dataFormat)
        Assert.assertEquals(null, result?.temperature)
        Assert.assertEquals(null, result?.pressure)
        Assert.assertEquals(null, result?.humidity)
        Assert.assertEquals(null, result?.accelX)
        Assert.assertEquals(null, result?.accelY)
        Assert.assertEquals(null, result?.accelZ)
        Assert.assertEquals(null, result?.txPower)
        Assert.assertEquals(null, result?.voltage)
        Assert.assertEquals(null, result?.movementCounter)
        Assert.assertEquals(null, result?.measurementSequenceNumber)
    }

}