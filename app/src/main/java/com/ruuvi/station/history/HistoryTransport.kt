package com.ruuvi.station.history

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** No HTTP/body logging, cookies or disk HTTP cache. Cancellation cancels the actual OkHttp call. */
class HistoryTransport(private val client: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(35, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()) {
    suspend fun load(query: HistoryQuery, endpoint: String, token: String): HistoryPage = withContext(Dispatchers.IO) {
        val wire = if (query.window.since == query.window.until)
            query.copy(window = HistoryWindow(maxOf(0, query.window.since - 1), maxOf(1, query.window.until))) else query
        val json = request(endpoint, "get", token, mapOf("sensor" to query.sensor, "since" to wire.window.since.toString(),
            "until" to wire.window.until.toString(), "limit" to HistoryQuery.PAGE_SIZE.toString(), "sort" to "desc", "mode" to query.mode.name.lowercase()))
        val page = try {
            val data = json.getAsJsonObject("data")
            val rows = data.getAsJsonArray("measurements").map { item ->
                val row = item.asJsonObject
                HistoryReading(integer(row.get("timestamp")), string(row.get("data")),
                    row.get("rssi")?.takeUnless { it.isJsonNull }?.let { integer(it).also { value -> if (value !in -128..127) throw HistoryFailure.Invalid() }.toInt() })
            }
            HistoryPage(string(data.get("sensor")), rows)
        } catch (_: Exception) { throw HistoryFailure.Invalid() }
        wire.validate(page)
        val result = page.copy(readings = page.readings.filter { it.timestamp in query.window })
        query.validate(result); result
    }
    suspend fun allowance(sensor: String, endpoint: String, token: String): Int = withContext(Dispatchers.IO) {
        val json = request(endpoint, "sensors-dense", token, mapOf("sensor" to sensor, "measurements" to "false", "alerts" to "false", "sharedToMe" to "true", "sharedToOthers" to "false"))
        try {
            val sensors = json.getAsJsonObject("data").getAsJsonArray("sensors")
            val item = sensors.firstOrNull { it.asJsonObject.get("sensor").asString.equals(sensor, true) } ?: throw HistoryFailure.Access()
            item.asJsonObject.getAsJsonObject("subscription").get("maxHistoryDays").asInt.coerceIn(0, HistoryWindow.MAX_DAYS)
        } catch (e: HistoryFailure.Access) { throw e }
        catch (_: Exception) { throw HistoryFailure.Invalid() }
    }
    private fun integer(value: JsonElement): Long = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asString?.toLongOrNull() ?: throw HistoryFailure.Invalid()
    private fun string(value: JsonElement): String = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString ?: throw HistoryFailure.Invalid()
    private suspend fun request(endpoint: String, route: String, token: String, parameters: Map<String, String>): JsonObject {
        val url = endpoint.toHttpUrl().newBuilder().addPathSegment(route)
        parameters.forEach { (key, value) -> url.addQueryParameter(key, value) }
        val request = Request.Builder().url(url.build()).header("Authorization", "Bearer $token")
            .header("Accept", "application/json").build()
        val bytes = suspendCancellableCoroutine<ByteArray> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWith(Result.failure(e)) }
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            if (it.code != 200) throw HistoryFailure.Http(it.code, retryAfter(it.header("Retry-After")))
                            val body = it.body ?: throw HistoryFailure.Invalid()
                            if (body.contentLength() > MAX_BYTES) throw HistoryFailure.Invalid()
                            body.byteStream().use { input ->
                                val output = ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val size = input.read(buffer)
                                    if (size < 0) break
                                    if (output.size() + size > MAX_BYTES) throw HistoryFailure.Invalid()
                                    output.write(buffer, 0, size)
                                }
                                output.toByteArray()
                            }
                        }
                    }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            })
        }
        return try {
            val parsed = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
            if (parsed.get("result")?.asString != "success") throw HistoryFailure.Invalid()
            parsed
        } catch (_: Exception) { throw HistoryFailure.Invalid() }
    }
    companion object {
        private const val MAX_BYTES = 4 * 1024 * 1024
        fun retryAfter(value: String?, now: Long = System.currentTimeMillis()): Long? {
            if (value == null) return null
            value.toDoubleOrNull()?.takeIf { it.isFinite() }?.let {
                return (it.coerceIn(0.0, Long.MAX_VALUE.toDouble() / 1000) * 1000).toLong()
            }
            return runCatching {
                val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                format.timeZone = TimeZone.getTimeZone("GMT")
                format.parse(value)?.time?.minus(now)?.coerceAtLeast(0)
            }.getOrNull()
        }
    }
}
