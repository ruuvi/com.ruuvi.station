package com.ruuvi.station.network.domain

import com.ruuvi.station.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor

private val accessTokenInBody = Regex("\"accessToken\"\\s*:\\s*\"[^\"]*\"")

/**
 * HTTP logging for debug builds only: release builds log nothing. Debug logs hide the session
 * token (Authorization header and accessToken in a body) and the sign-in code and e-mail sent in
 * the URL. Other body content is logged as it is.
 */
fun createHttpLoggingInterceptor(
    debug: Boolean = BuildConfig.DEBUG,
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
): HttpLoggingInterceptor = HttpLoggingInterceptor { message ->
    logger.log(message.replace(accessTokenInBody, "\"accessToken\":\"██\""))
}.also {
    it.level = if (debug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    it.redactHeader("Authorization")
    it.redactQueryParams("token", "email")
}
