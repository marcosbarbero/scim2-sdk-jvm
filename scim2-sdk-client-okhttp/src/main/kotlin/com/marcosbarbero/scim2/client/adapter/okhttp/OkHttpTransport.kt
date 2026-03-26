/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marcosbarbero.scim2.client.adapter.okhttp

import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient(),
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
            body = responseBody,
        )
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
