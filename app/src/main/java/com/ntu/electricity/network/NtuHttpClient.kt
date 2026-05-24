package com.ntu.electricity.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NtuHttpClient(context: Context) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        private const val TIMEOUT_SECONDS = 20L
    }

    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private fun baseHeaders(): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
        "Connection" to "keep-alive",
        "Cache-Control" to "no-cache",
    )

    fun get(url: String, additionalHeaders: Map<String, String> = emptyMap()): okhttp3.Response {
        val requestBuilder = Request.Builder().url(url).get()
        baseHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        additionalHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        return client.newCall(requestBuilder.build()).execute()
    }

    fun post(
        url: String,
        formBody: Map<String, String>,
        additionalHeaders: Map<String, String> = emptyMap()
    ): okhttp3.Response {
        val formBodyString = formBody.entries.joinToString("&") {
            "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
        }
        val requestBody = formBodyString.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        baseHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        additionalHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        return client.newCall(requestBuilder.build()).execute()
    }

    fun getRedirectLocation(response: okhttp3.Response): String? {
        return response.header("Location")
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
