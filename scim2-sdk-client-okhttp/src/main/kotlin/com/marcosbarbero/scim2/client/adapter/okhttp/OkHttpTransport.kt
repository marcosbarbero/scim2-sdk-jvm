package com.marcosbarbero.scim2.client.adapter.okhttp

import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient()
) : HttpTransport {

    override fun execute(request: HttpRequest): HttpResponse {
        val contentType = request.headers["Content-Type"]?.toMediaTypeOrNull()
        val bodyBytes = request.body

        val requestBody = when {
            bodyBytes != null -> bodyBytes.toRequestBody(contentType)
            request.method in listOf("POST", "PUT", "PATCH") -> ByteArray(0).toRequestBody(contentType)
            else -> null
        }

        val builder = Request.Builder()
            .url(request.url)
            .method(request.method, requestBody)

        request.headers.forEach { (key, value) ->
            builder.header(key, value)
        }

        val okResponse = client.newCall(builder.build()).execute()

        val responseHeaders = mutableMapOf<String, List<String>>()
        for (name in okResponse.headers.names()) {
            responseHeaders[name] = okResponse.headers.values(name)
        }

        val responseBody = okResponse.body?.bytes()?.takeIf { it.isNotEmpty() }

        return HttpResponse(
            statusCode = okResponse.code,
            headers = responseHeaders,
            body = responseBody
        )
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
