package com.ruuvi.station.history

import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URLDecoder

class HistoryTransportTest {
    private val sensor = "FA:00:00:00:00:01"
    @Test fun `real HTTP transport uses existing GET pagination contract and sensor owner allowance`() = runBlocking {
        val requests = mutableListOf<Map<String, String>>()
        val server = ServerSocket(0)
        val worker = thread(isDaemon = true) {
            repeat(2) { server.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                val requestLine = input.readLine()
                val uri = java.net.URI(requestLine.split(' ')[1])
                val headers = mutableMapOf<String,String>()
                while (true) { val line = input.readLine() ?: break; if (line.isEmpty()) break
                    headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                }
                synchronized(requests) { requests += uri.rawQuery.split('&').associate {
                    val pair = it.split('=', limit=2); URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair[1], "UTF-8")
                } + ("authorization" to headers.getValue("authorization")) }
                val body = if (uri.path == "/get") """{"result":"success","data":{"sensor":"$sensor","measurements":[{"timestamp":10,"data":"05ff","rssi":null}]}}"""
                    else """{"result":"success","data":{"sensors":[{"sensor":"$sensor","subscription":{"maxHistoryDays":1096}}]}}"""
                val bytes=body.toByteArray()
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(bytes); flush()
                }
            } }
        }
        try {
            val endpoint = "http://127.0.0.1:${server.localPort}/"
            val transport = HistoryTransport()
            val page = transport.load(HistoryQuery(sensor,HistoryWindow(10,10),HistoryMode.SPARSE), endpoint,"synthetic")
            assertEquals(1,page.readings.size); assertNull(page.readings.single().rssi)
            assertEquals(1096,transport.allowance(sensor, endpoint,"synthetic"))
            assertEquals(mapOf("sensor" to sensor,"since" to "9","until" to "10","limit" to "1500","sort" to "desc","mode" to "sparse", "authorization" to "Bearer synthetic"),requests.first())
        } finally { server.close(); worker.join(1000) }
    }
    @Test fun `malformed duplicate or out of window rows cannot mark coverage complete`() {
        val query = HistoryQuery(sensor,HistoryWindow(10,20),HistoryMode.DENSE)
        for (rows in listOf(listOf(HistoryReading(21,"00",null)),listOf(HistoryReading(10,"00",null),HistoryReading(10,"00",null)),listOf(HistoryReading(15,"garbage",null)))) {
            try { query.validate(HistoryPage(sensor, rows)); fail() } catch (_: HistoryFailure.Invalid) { }
        }
        assertEquals(12000L,HistoryTransport.retryAfter("12"))
        assertEquals(30000L,HistoryTransport.retryAfter("Thu, 01 Jan 1970 00:00:30 GMT",0))
    }
}
