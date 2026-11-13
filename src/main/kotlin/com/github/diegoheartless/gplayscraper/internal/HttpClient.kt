package com.github.diegoheartless.gplayscraper.internal

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around [OkHttpClient] used by every endpoint.
 *
 * It is deliberately small: the scraper only ever needs GET requests returning
 * HTML/JSON and form-encoded POSTs against the Play Store `batchexecute`
 * endpoint, both of which follow redirects by default.
 */
internal class HttpClient(
    private val client: OkHttpClient = DefaultClient
) {

    /** Performs a GET request and returns the response body as text. */
    fun get(url: String): String {
        val request = Request.Builder().url(url).header("Accept-Language", "en").build()
        return execute(request)
    }

    /** Performs a form-encoded POST request and returns the response body as text. */
    fun post(url: String, formFields: Map<String, String>): String {
        val builder = FormBody.Builder()
        formFields.forEach { (k, v) -> builder.add(k, v) }
        val request = Request.Builder()
            .url(url)
            .post(builder.build())
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP ${response.code} for ${request.url}")
            }
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        private val DefaultClient: OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        /** Builds a URL with the given query parameters appended. */
        fun url(url: String, params: Map<String, String?>): String {
            val builder = HttpUrl.get(url).newBuilder()
            params.forEach { (k, v) -> if (v != null) builder.addQueryParameter(k, v) }
            return builder.build().toString()
        }
    }
}
