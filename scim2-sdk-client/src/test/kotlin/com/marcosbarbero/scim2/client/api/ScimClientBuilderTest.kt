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
package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.client.port.BearerTokenAuthentication
import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ScimClientBuilderTest {

    private val faker = Faker()

    @Test
    fun `build throws when baseUrl is not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ScimClientBuilder()
                .transport(mockk<HttpTransport>())
                .serializer(mockk<ScimSerializer>())
                .build()
        }
        exception.message shouldContain "baseUrl"
    }

    @Test
    fun `build throws when transport is not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ScimClientBuilder()
                .baseUrl("https://example.com")
                .serializer(mockk<ScimSerializer>())
                .build()
        }
        exception.message shouldContain "transport"
    }

    @Test
    fun `build throws when serializer is not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ScimClientBuilder()
                .baseUrl("https://example.com")
                .transport(mockk<HttpTransport>())
                .build()
        }
        exception.message shouldContain "serializer"
    }

    @Test
    fun `build succeeds with all required fields`() {
        val client = ScimClientBuilder()
            .baseUrl("https://example.com")
            .transport(object : HttpTransport {
                override fun execute(request: HttpRequest): HttpResponse = HttpResponse(200)
                override fun close() {}
            })
            .serializer(mockk<ScimSerializer>())
            .build()

        client shouldNotBe null
    }

    @Test
    fun `build succeeds with optional authentication and default headers`() {
        val token = faker.random.randomString(32)
        val client = ScimClientBuilder()
            .baseUrl("https://example.com")
            .transport(object : HttpTransport {
                override fun execute(request: HttpRequest): HttpResponse = HttpResponse(200)
                override fun close() {}
            })
            .serializer(mockk<ScimSerializer>())
            .authentication(BearerTokenAuthentication(token))
            .defaultHeaders(mapOf("X-Tenant" to "acme"))
            .build()

        client shouldNotBe null
    }
}
