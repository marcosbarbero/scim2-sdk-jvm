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
package com.marcosbarbero.scim2.client.adapter.httpclient

import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration

class HttpClientTransport(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val requestTimeout: Duration? = null
) : HttpTransport {

    override fun execute(request: HttpRequest): HttpResponse {
        val bodyPublisher = if (request.body != null) {
            java.net.http.HttpRequest.BodyPublishers.ofByteArray(request.body)
        } else {
            java.net.http.HttpRequest.BodyPublishers.noBody()
        }

        val builder = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(request.url))
            .method(request.method, bodyPublisher)

        requestTimeout?.let { builder.timeout(it) }

        request.headers.forEach { (key, value) ->
            builder.header(key, value)
        }

        val javaRequest = builder.build()
        val javaResponse = httpClient.send(javaRequest, java.net.http.HttpResponse.BodyHandlers.ofByteArray())

        val responseHeaders = javaResponse.headers().map().mapValues { (_, values) -> values.toList() }
        val responseBody = javaResponse.body()?.takeIf { it.isNotEmpty() }

        return HttpResponse(
            statusCode = javaResponse.statusCode(),
            headers = responseHeaders,
            body = responseBody
        )
    }

    override fun close() {
        // Java HttpClient does not require explicit close
    }
}
