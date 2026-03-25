package com.marcosbarbero.scim2.client.port

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.util.Base64

class BasicAuthenticationTest {

    private val faker = Faker()

    @Test
    fun `authenticate adds Basic auth header with Base64 encoded credentials`() {
        val username = faker.name.firstName()
        val password = faker.random.randomString(16)
        val auth = BasicAuthentication(username, password)
        val request = HttpRequest(method = "GET", url = "https://example.com/Users")

        val result = auth.authenticate(request)

        val expected = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        result.headers["Authorization"] shouldBe "Basic $expected"
    }

    @Test
    fun `authenticate preserves existing headers`() {
        val auth = BasicAuthentication("user", "pass")
        val request = HttpRequest(
            method = "GET",
            url = "https://example.com/Users",
            headers = mapOf("Accept" to "application/json")
        )

        val result = auth.authenticate(request)

        result.headers["Accept"] shouldBe "application/json"
        result.headers["Authorization"] shouldStartWith "Basic "
    }

    @Test
    fun `authenticate handles special characters in credentials`() {
        val username = "user@domain.com"
        val password = "p@ss:w0rd!"
        val auth = BasicAuthentication(username, password)
        val request = HttpRequest(method = "GET", url = "https://example.com/Users")

        val result = auth.authenticate(request)

        val decoded = String(Base64.getDecoder().decode(result.headers["Authorization"]!!.removePrefix("Basic ")))
        decoded shouldBe "$username:$password"
    }
}
