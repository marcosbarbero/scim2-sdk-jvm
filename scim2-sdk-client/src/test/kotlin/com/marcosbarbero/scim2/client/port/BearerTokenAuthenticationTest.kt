package com.marcosbarbero.scim2.client.port

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class BearerTokenAuthenticationTest {

    private val faker = Faker()

    @Test
    fun `authenticate adds Bearer token to Authorization header`() {
        val token = faker.random.randomString(32)
        val auth = BearerTokenAuthentication(token)
        val request = HttpRequest(method = "GET", url = "https://example.com/Users")

        val result = auth.authenticate(request)

        result.headers["Authorization"] shouldBe "Bearer $token"
    }

    @Test
    fun `authenticate preserves existing headers`() {
        val token = faker.random.randomString(32)
        val auth = BearerTokenAuthentication(token)
        val existingHeaders = mapOf("X-Custom" to "value")
        val request = HttpRequest(method = "GET", url = "https://example.com/Users", headers = existingHeaders)

        val result = auth.authenticate(request)

        result.headers["X-Custom"] shouldBe "value"
        result.headers["Authorization"] shouldStartWith "Bearer "
    }

    @Test
    fun `authenticate does not modify original request`() {
        val token = faker.random.randomString(32)
        val auth = BearerTokenAuthentication(token)
        val request = HttpRequest(method = "GET", url = "https://example.com/Users")

        auth.authenticate(request)

        request.headers shouldBe emptyMap()
    }
}
