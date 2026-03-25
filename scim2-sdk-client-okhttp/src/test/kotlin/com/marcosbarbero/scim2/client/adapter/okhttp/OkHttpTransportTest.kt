package com.marcosbarbero.scim2.client.adapter.okhttp

import com.marcosbarbero.scim2.client.port.HttpRequest
import com.sun.net.httpserver.HttpServer
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

class OkHttpTransportTest {

    private val faker = Faker()
    private lateinit var server: HttpServer
    private lateinit var transport: OkHttpTransport
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        port = server.address.port
        transport = OkHttpTransport()
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
        transport.close()
    }

    @Test
    fun `execute sends GET request and returns response`() {
        val responseText = faker.random.randomString(20)
        server.createContext("/test") { exchange ->
            val body = responseText.toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        val request = HttpRequest(method = "GET", url = "http://localhost:$port/test")
        val response = transport.execute(request)

        response.statusCode shouldBe 200
        response.body shouldNotBe null
        String(response.body!!) shouldBe responseText
    }

    @Test
    fun `execute sends POST request with body`() {
        val requestText = faker.random.randomString(30)
        var receivedBody: String? = null
        var receivedContentType: String? = null

        server.createContext("/users") { exchange ->
            receivedBody = exchange.requestBody.bufferedReader().readText()
            receivedContentType = exchange.requestHeaders.getFirst("Content-Type")
            val responseBody = """{"id":"1"}""".toByteArray()
            exchange.sendResponseHeaders(201, responseBody.size.toLong())
            exchange.responseBody.use { it.write(responseBody) }
        }
        server.start()

        val request = HttpRequest(
            method = "POST",
            url = "http://localhost:$port/users",
            headers = mapOf("Content-Type" to "application/scim+json"),
            body = requestText.toByteArray()
        )
        val response = transport.execute(request)

        response.statusCode shouldBe 201
        receivedBody shouldBe requestText
        receivedContentType shouldBe "application/scim+json"
    }

    @Test
    fun `execute maps response headers correctly`() {
        server.createContext("/headers") { exchange ->
            exchange.responseHeaders.add("X-Custom", "value1")
            exchange.responseHeaders.add("ETag", "W/\"abc\"")
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()

        val request = HttpRequest(method = "GET", url = "http://localhost:$port/headers")
        val response = transport.execute(request)

        response.statusCode shouldBe 200
        response.headers.entries.firstOrNull { it.key.equals("X-Custom", ignoreCase = true) }?.value?.first() shouldBe "value1"
        response.headers.entries.firstOrNull { it.key.equals("ETag", ignoreCase = true) }?.value?.first() shouldBe "W/\"abc\""
    }

    @Test
    fun `execute sends request headers to server`() {
        var receivedAuth: String? = null
        server.createContext("/auth") { exchange ->
            receivedAuth = exchange.requestHeaders.getFirst("Authorization")
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()

        val request = HttpRequest(method = "GET", url = "http://localhost:$port/auth", headers = mapOf("Authorization" to "Bearer my-token"))
        transport.execute(request)

        receivedAuth shouldBe "Bearer my-token"
    }

    @Test
    fun `execute handles DELETE request`() {
        var receivedMethod: String? = null
        server.createContext("/users/123") { exchange ->
            receivedMethod = exchange.requestMethod
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()

        val request = HttpRequest(method = "DELETE", url = "http://localhost:$port/users/123")
        val response = transport.execute(request)

        response.statusCode shouldBe 204
        receivedMethod shouldBe "DELETE"
    }

    @Test
    fun `execute handles PUT request with body`() {
        var receivedBody: String? = null
        server.createContext("/users/456") { exchange ->
            receivedBody = exchange.requestBody.bufferedReader().readText()
            val responseBody = """{"id":"456"}""".toByteArray()
            exchange.sendResponseHeaders(200, responseBody.size.toLong())
            exchange.responseBody.use { it.write(responseBody) }
        }
        server.start()

        val request = HttpRequest(
            method = "PUT",
            url = "http://localhost:$port/users/456",
            headers = mapOf("Content-Type" to "application/scim+json"),
            body = """{"userName":"updated"}""".toByteArray()
        )
        val response = transport.execute(request)

        response.statusCode shouldBe 200
        receivedBody shouldBe """{"userName":"updated"}"""
    }
}
