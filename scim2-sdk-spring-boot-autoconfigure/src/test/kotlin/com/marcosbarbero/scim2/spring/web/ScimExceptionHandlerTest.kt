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
package com.marcosbarbero.scim2.spring.web

import com.marcosbarbero.scim2.core.domain.model.error.InvalidFilterException
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ScimError
import com.marcosbarbero.scim2.core.domain.model.error.ScimProblemDetail
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

class ScimExceptionHandlerTest {

    private val faker = Faker()
    private val objectMapper = jacksonObjectMapper()

    private val serializer = object : ScimSerializer {
        override fun <T : Any> serialize(value: T): ByteArray = objectMapper.writeValueAsBytes(value)
        override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T = objectMapper.readValue(bytes, type.java)
        override fun serializeToString(value: Any): String = objectMapper.writeValueAsString(value)
        override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T = objectMapper.readValue(json, type.java)
        override fun enrichMetaLocation(json: ByteArray, location: String, resourceType: String?): ByteArray = json
    }

    private val handler = ScimExceptionHandler(serializer)

    @Test
    fun `returns ScimError for application scim+json Accept`() {
        val detail = faker.name.name()
        val ex = ResourceNotFoundException(detail)
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("Accept") } returns "application/scim+json"

        val response = handler.handleScimException(ex, request)

        response.statusCode.value() shouldBe 404
        response.headers.contentType.toString() shouldBe "application/scim+json"
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.status shouldBe "404"
        error.detail shouldBe detail
    }

    @Test
    fun `returns ProblemDetail for application problem+json Accept`() {
        val detail = faker.name.name()
        val ex = InvalidFilterException(detail)
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("Accept") } returns "application/problem+json"

        val response = handler.handleScimException(ex, request)

        response.statusCode.value() shouldBe 400
        response.headers.contentType.toString() shouldBe "application/problem+json"
        val problemDetail = objectMapper.readValue(response.body, ScimProblemDetail::class.java)
        problemDetail.status shouldBe 400
        problemDetail.detail shouldBe detail
        problemDetail.scimType shouldBe "invalidFilter"
        problemDetail.type shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:invalidFilter"
    }

    @Test
    fun `returns ScimError by default when no Accept header`() {
        val detail = faker.name.name()
        val ex = ResourceNotFoundException(detail)
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("Accept") } returns null

        val response = handler.handleScimException(ex, request)

        response.statusCode.value() shouldBe 404
        response.headers.contentType.toString() shouldBe "application/scim+json"
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.status shouldBe "404"
        error.detail shouldBe detail
    }
}
