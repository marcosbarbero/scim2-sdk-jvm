package com.marcosbarbero.scim2.client.adapter.httpclient

import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import java.net.URI
import java.net.http.HttpClient

class JavaHttpClientTransport(
    private val httpClient: HttpClient = HttpClient.newHttpClient()
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
