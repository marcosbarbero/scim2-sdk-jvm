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
package com.marcosbarbero.scim2.client.port

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HttpTransportTest {

    private val faker = Faker()

    @Nested
    inner class HttpRequestTest {

        @Test
        fun `equals should return true for identical requests`() {
            val url = faker.internet.domain()
            val a = HttpRequest(method = "GET", url = url, body = "hello".toByteArray())
            val b = HttpRequest(method = "GET", url = url, body = "hello".toByteArray())

            (a == b) shouldBe true
        }

        @Test
        fun `equals should return false for different method`() {
            val url = faker.internet.domain()
            val a = HttpRequest(method = "GET", url = url)
            val b = HttpRequest(method = "POST", url = url)

            (a == b) shouldBe false
        }

        @Test
        fun `equals should return false for different body`() {
            val url = faker.internet.domain()
            val a = HttpRequest(method = "POST", url = url, body = "a".toByteArray())
            val b = HttpRequest(method = "POST", url = url, body = "b".toByteArray())

            (a == b) shouldBe false
        }

        @Test
        fun `equals should return false for non-HttpRequest`() {
            val request = HttpRequest(method = "GET", url = "http://example.com")

            (request.equals("not a request")) shouldBe false
        }

        @Test
        fun `equals should return true for same reference`() {
            val request = HttpRequest(method = "GET", url = "http://example.com")

            (request == request) shouldBe true
        }

        @Test
        fun `hashCode should be consistent for equal objects`() {
            val url = faker.internet.domain()
            val a = HttpRequest(method = "GET", url = url, body = "test".toByteArray())
            val b = HttpRequest(method = "GET", url = url, body = "test".toByteArray())

            a.hashCode() shouldBe b.hashCode()
        }

        @Test
        fun `hashCode should handle null body`() {
            val request = HttpRequest(method = "GET", url = "http://example.com")

            request.hashCode() shouldBe request.hashCode()
        }

        @Test
        fun `should carry headers`() {
            val request = HttpRequest(
                method = "GET",
                url = "http://example.com",
                headers = mapOf("Authorization" to "Bearer token"),
            )

            request.headers["Authorization"] shouldBe "Bearer token"
        }
    }

    @Nested
    inner class HttpResponseTest {

        @Test
        fun `equals should return true for identical responses`() {
            val a = HttpResponse(statusCode = 200, body = "ok".toByteArray())
            val b = HttpResponse(statusCode = 200, body = "ok".toByteArray())

            (a == b) shouldBe true
        }

        @Test
        fun `equals should return false for different status`() {
            val a = HttpResponse(statusCode = 200)
            val b = HttpResponse(statusCode = 404)

            (a == b) shouldBe false
        }

        @Test
        fun `equals should return false for different body`() {
            val a = HttpResponse(statusCode = 200, body = "a".toByteArray())
            val b = HttpResponse(statusCode = 200, body = "b".toByteArray())

            (a == b) shouldBe false
        }

        @Test
        fun `equals should return false for non-HttpResponse`() {
            val response = HttpResponse(statusCode = 200)

            (response.equals("not a response")) shouldBe false
        }

        @Test
        fun `equals should return true for same reference`() {
            val response = HttpResponse(statusCode = 200)

            (response == response) shouldBe true
        }

        @Test
        fun `hashCode should be consistent for equal objects`() {
            val a = HttpResponse(statusCode = 200, body = "ok".toByteArray())
            val b = HttpResponse(statusCode = 200, body = "ok".toByteArray())

            a.hashCode() shouldBe b.hashCode()
        }

        @Test
        fun `hashCode should handle null body`() {
            val response = HttpResponse(statusCode = 204)

            response.hashCode() shouldBe response.hashCode()
        }

        @Test
        fun `should carry headers`() {
            val response = HttpResponse(
                statusCode = 200,
                headers = mapOf("Content-Type" to listOf("application/json")),
            )

            response.headers["Content-Type"] shouldBe listOf("application/json")
        }
    }
}
