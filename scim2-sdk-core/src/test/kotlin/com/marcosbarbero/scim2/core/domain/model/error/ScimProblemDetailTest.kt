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
package com.marcosbarbero.scim2.core.domain.model.error

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class ScimProblemDetailTest {

    private val faker = Faker()
    private val mapper = jacksonObjectMapper()

    @Nested
    inner class FromScimExceptionTest {

        @Test
        fun `fromScimException maps all fields correctly`() {
            val detail = faker.name.name()
            val ex = InvalidFilterException(detail)

            val problemDetail = ScimProblemDetail.fromScimException(ex)

            problemDetail.status shouldBe 400
            problemDetail.detail shouldBe detail
            problemDetail.scimType shouldBe "invalidFilter"
            problemDetail.type shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:invalidFilter"
            problemDetail.title shouldBe "Invalid filter"
            problemDetail.instance shouldBe null
        }

        @Test
        fun `fromScimException with scimType generates type URI`() {
            val ex = ResourceConflictException("duplicate")

            val problemDetail = ScimProblemDetail.fromScimException(ex)

            problemDetail.type shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:uniqueness"
            problemDetail.scimType shouldBe "uniqueness"
            problemDetail.title shouldBe "Uniqueness"
        }

        @Test
        fun `fromScimException without scimType uses about blank`() {
            val detail = faker.name.name()
            val ex = ResourceNotFoundException(detail)

            val problemDetail = ScimProblemDetail.fromScimException(ex)

            problemDetail.type shouldBe "about:blank"
            problemDetail.scimType shouldBe null
            problemDetail.title shouldBe "Not Found"
            problemDetail.status shouldBe 404
            problemDetail.detail shouldBe detail
        }

        @Test
        fun `toScimProblemDetail on ScimException delegates to fromScimException`() {
            val detail = faker.name.name()
            val ex = InvalidSyntaxException(detail)

            val problemDetail = ex.toScimProblemDetail()

            problemDetail.status shouldBe 400
            problemDetail.scimType shouldBe "invalidSyntax"
            problemDetail.type shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:invalidSyntax"
        }
    }

    @Nested
    inner class FromScimErrorTest {

        @Test
        fun `fromScimError round-trip preserves fields`() {
            val detail = faker.name.name()
            val error = ScimError(
                status = "409",
                scimType = "uniqueness",
                detail = detail,
            )

            val problemDetail = ScimProblemDetail.fromScimError(error)

            problemDetail.status shouldBe 409
            problemDetail.scimType shouldBe "uniqueness"
            problemDetail.detail shouldBe detail
            problemDetail.type shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:uniqueness"
            problemDetail.title shouldBe "uniqueness"
        }

        @Test
        fun `fromScimError without scimType uses about blank`() {
            val error = ScimError(
                status = "404",
                detail = "Resource not found",
            )

            val problemDetail = ScimProblemDetail.fromScimError(error)

            problemDetail.type shouldBe "about:blank"
            problemDetail.title shouldBe "Not Found"
            problemDetail.scimType shouldBe null
        }

        @Test
        fun `fromScimError with invalid status defaults to 500`() {
            val error = ScimError(
                status = "invalid",
                detail = "Something went wrong",
            )

            val problemDetail = ScimProblemDetail.fromScimError(error)

            problemDetail.status shouldBe 500
            problemDetail.title shouldBe "Internal Server Error"
        }
    }

    @Nested
    inner class HttpStatusTitleTest {

        @Test
        fun `httpStatusTitle maps common codes`() {
            ScimProblemDetail.httpStatusTitle(400) shouldBe "Bad Request"
            ScimProblemDetail.httpStatusTitle(401) shouldBe "Unauthorized"
            ScimProblemDetail.httpStatusTitle(403) shouldBe "Forbidden"
            ScimProblemDetail.httpStatusTitle(404) shouldBe "Not Found"
            ScimProblemDetail.httpStatusTitle(409) shouldBe "Conflict"
            ScimProblemDetail.httpStatusTitle(412) shouldBe "Precondition Failed"
            ScimProblemDetail.httpStatusTitle(413) shouldBe "Payload Too Large"
            ScimProblemDetail.httpStatusTitle(500) shouldBe "Internal Server Error"
            ScimProblemDetail.httpStatusTitle(501) shouldBe "Not Implemented"
            ScimProblemDetail.httpStatusTitle(418) shouldBe "Error"
        }
    }

    @Nested
    inner class JsonSerializationTest {

        @Test
        fun `JSON serialization includes non-null fields only`() {
            val problemDetail = ScimProblemDetail(
                status = 404,
                detail = "User not found",
                title = "Not Found",
            )

            val json = mapper.writeValueAsString(problemDetail)
            val tree = mapper.readTree(json)

            tree.has("status") shouldBe true
            tree.has("detail") shouldBe true
            tree.has("title") shouldBe true
            tree.has("type") shouldBe true
            tree.get("type").stringValue() shouldBe "about:blank"
            // null fields should be absent
            tree.has("instance") shouldBe false
            tree.has("scimType") shouldBe false
        }

        @Test
        fun `JSON serialization includes scimType when present`() {
            val problemDetail = ScimProblemDetail(
                type = "urn:ietf:params:scim:api:messages:2.0:Error:invalidFilter",
                status = 400,
                detail = "Bad filter",
                title = "Invalid filter",
                scimType = "invalidFilter",
            )

            val json = mapper.writeValueAsString(problemDetail)
            val tree = mapper.readTree(json)

            tree.get("scimType").stringValue() shouldBe "invalidFilter"
            tree.get("type").stringValue() shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:invalidFilter"
        }

        @Test
        fun `JSON round-trip deserialization preserves all fields`() {
            val original = ScimProblemDetail(
                type = "urn:ietf:params:scim:api:messages:2.0:Error:uniqueness",
                title = "Uniqueness",
                status = 409,
                detail = "User already exists",
                instance = "/Users/123",
                scimType = "uniqueness",
            )

            val json = mapper.writeValueAsString(original)
            val deserialized = mapper.readValue<ScimProblemDetail>(json)

            deserialized shouldBe original
        }
    }
}
