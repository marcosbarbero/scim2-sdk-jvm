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

import com.marcosbarbero.scim2.core.domain.ScimUrns
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScimErrorTest {

    private val faker = Faker()

    @Nested
    inner class ScimExceptionHierarchyTest {

        @Test
        fun `ResourceNotFoundException should have status 404`() {
            val detail = faker.name.name()
            val ex = ResourceNotFoundException(detail)
            ex.status shouldBe 404
            ex.detail shouldBe detail
            ex.scimType.shouldBe(null)
        }

        @Test
        fun `ResourceConflictException should have status 409`() {
            val detail = faker.name.name()
            val ex = ResourceConflictException(detail)
            ex.status shouldBe 409
            ex.scimType shouldBe ScimErrorType.UNIQUENESS
        }

        @Test
        fun `InvalidFilterException should have status 400 and scimType invalidFilter`() {
            val detail = faker.name.name()
            val ex = InvalidFilterException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_FILTER
        }

        @Test
        fun `InvalidPathException should have status 400 and scimType invalidPath`() {
            val detail = faker.name.name()
            val ex = InvalidPathException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_PATH
        }

        @Test
        fun `MutabilityException should have status 400 and scimType mutability`() {
            val detail = faker.name.name()
            val ex = MutabilityException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.MUTABILITY
        }

        @Test
        fun `InvalidSyntaxException should have status 400 and scimType invalidSyntax`() {
            val detail = faker.name.name()
            val ex = InvalidSyntaxException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_SYNTAX
        }

        @Test
        fun `NoTargetException should have status 400 and scimType noTarget`() {
            val detail = faker.name.name()
            val ex = NoTargetException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.NO_TARGET
        }

        @Test
        fun `InvalidValueException should have status 400 and scimType invalidValue`() {
            val detail = faker.name.name()
            val ex = InvalidValueException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_VALUE
        }

        @Test
        fun `TooManyException should have status 400 and scimType tooMany`() {
            val detail = faker.name.name()
            val ex = TooManyException(detail)
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.TOO_MANY
        }

        @Test
        fun `SensitiveException should have status 403 and scimType sensitive`() {
            val detail = faker.name.name()
            val ex = SensitiveException(detail)
            ex.status shouldBe 403
            ex.scimType shouldBe ScimErrorType.SENSITIVE
        }

        @Test
        fun `all exceptions should extend ScimException`() {
            val detail = faker.name.name()
            val exceptions: List<ScimException> = listOf(
                ResourceNotFoundException(detail),
                ResourceConflictException(detail),
                InvalidFilterException(detail),
                InvalidPathException(detail),
                MutabilityException(detail),
                InvalidSyntaxException(detail),
                NoTargetException(detail),
                InvalidValueException(detail),
                TooManyException(detail),
                SensitiveException(detail)
            )
            exceptions.forEach { it.shouldBeInstanceOf<ScimException>() }
        }
    }

    @Nested
    inner class ScimErrorTypeTest {

        @Test
        fun `enum values should match RFC 7644 wire values`() {
            ScimErrorType.INVALID_FILTER.value shouldBe "invalidFilter"
            ScimErrorType.TOO_MANY.value shouldBe "tooMany"
            ScimErrorType.UNIQUENESS.value shouldBe "uniqueness"
            ScimErrorType.MUTABILITY.value shouldBe "mutability"
            ScimErrorType.INVALID_SYNTAX.value shouldBe "invalidSyntax"
            ScimErrorType.INVALID_PATH.value shouldBe "invalidPath"
            ScimErrorType.NO_TARGET.value shouldBe "noTarget"
            ScimErrorType.INVALID_VALUE.value shouldBe "invalidValue"
            ScimErrorType.INVALID_VERS.value shouldBe "invalidVers"
            ScimErrorType.SENSITIVE.value shouldBe "sensitive"
        }
    }

    @Nested
    inner class ScimErrorWireFormatTest {

        private val mapper: ObjectMapper = jacksonObjectMapper()

        @Test
        fun `should serialize ScimError to JSON`() {
            val detail = faker.name.name()
            val error = ScimError(
                schemas = listOf(ScimUrns.ERROR),
                status = "400",
                scimType = "invalidFilter",
                detail = detail
            )

            val json = mapper.writeValueAsString(error)
            val deserialized = mapper.readValue<ScimError>(json)

            deserialized.status shouldBe "400"
            deserialized.scimType shouldBe "invalidFilter"
            deserialized.detail shouldBe detail
            deserialized.schemas shouldBe listOf(ScimUrns.ERROR)
        }

        @Test
        fun `should deserialize ScimError from JSON`() {
            val detail = faker.name.name()
            val json = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
                "status": "404",
                "detail": "$detail"
            }
            """.trimIndent()

            val error = mapper.readValue<ScimError>(json)
            error.status shouldBe "404"
            error.scimType shouldBe null
            error.detail shouldBe detail
        }

        @Test
        fun `ScimException should convert to ScimError`() {
            val detail = faker.name.name()
            val ex = InvalidFilterException(detail)
            val error = ex.toScimError()

            error.status shouldBe "400"
            error.scimType shouldBe "invalidFilter"
            error.detail shouldBe detail
            error.schemas shouldBe listOf(ScimUrns.ERROR)
        }
    }
}
