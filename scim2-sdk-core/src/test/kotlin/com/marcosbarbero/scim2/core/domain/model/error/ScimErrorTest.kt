package com.marcosbarbero.scim2.core.domain.model.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScimErrorTest {

    @Nested
    inner class ScimExceptionHierarchyTest {

        @Test
        fun `ResourceNotFoundException should have status 404`() {
            val ex = ResourceNotFoundException("User not found")
            ex.status shouldBe 404
            ex.detail shouldBe "User not found"
            ex.scimType.shouldBe(null)
        }

        @Test
        fun `ResourceConflictException should have status 409`() {
            val ex = ResourceConflictException("User already exists")
            ex.status shouldBe 409
            ex.scimType shouldBe ScimErrorType.UNIQUENESS
        }

        @Test
        fun `InvalidFilterException should have status 400 and scimType invalidFilter`() {
            val ex = InvalidFilterException("Bad filter")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_FILTER
        }

        @Test
        fun `InvalidPathException should have status 400 and scimType invalidPath`() {
            val ex = InvalidPathException("Bad path")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_PATH
        }

        @Test
        fun `MutabilityException should have status 400 and scimType mutability`() {
            val ex = MutabilityException("Cannot modify read-only attribute")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.MUTABILITY
        }

        @Test
        fun `InvalidSyntaxException should have status 400 and scimType invalidSyntax`() {
            val ex = InvalidSyntaxException("Bad syntax")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_SYNTAX
        }

        @Test
        fun `NoTargetException should have status 400 and scimType noTarget`() {
            val ex = NoTargetException("No target")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.NO_TARGET
        }

        @Test
        fun `InvalidValueException should have status 400 and scimType invalidValue`() {
            val ex = InvalidValueException("Bad value")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.INVALID_VALUE
        }

        @Test
        fun `TooManyException should have status 400 and scimType tooMany`() {
            val ex = TooManyException("Too many results")
            ex.status shouldBe 400
            ex.scimType shouldBe ScimErrorType.TOO_MANY
        }

        @Test
        fun `SensitiveException should have status 403 and scimType sensitive`() {
            val ex = SensitiveException("Sensitive attribute")
            ex.status shouldBe 403
            ex.scimType shouldBe ScimErrorType.SENSITIVE
        }

        @Test
        fun `all exceptions should extend ScimException`() {
            val exceptions: List<ScimException> = listOf(
                ResourceNotFoundException("test"),
                ResourceConflictException("test"),
                InvalidFilterException("test"),
                InvalidPathException("test"),
                MutabilityException("test"),
                InvalidSyntaxException("test"),
                NoTargetException("test"),
                InvalidValueException("test"),
                TooManyException("test"),
                SensitiveException("test")
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
            val error = ScimError(
                schemas = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
                status = "400",
                scimType = "invalidFilter",
                detail = "The filter syntax is invalid"
            )

            val json = mapper.writeValueAsString(error)
            val deserialized = mapper.readValue<ScimError>(json)

            deserialized.status shouldBe "400"
            deserialized.scimType shouldBe "invalidFilter"
            deserialized.detail shouldBe "The filter syntax is invalid"
            deserialized.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:Error")
        }

        @Test
        fun `should deserialize ScimError from JSON`() {
            val json = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
                "status": "404",
                "detail": "Resource not found"
            }
            """.trimIndent()

            val error = mapper.readValue<ScimError>(json)
            error.status shouldBe "404"
            error.scimType shouldBe null
            error.detail shouldBe "Resource not found"
        }

        @Test
        fun `ScimException should convert to ScimError`() {
            val ex = InvalidFilterException("Bad filter expression")
            val error = ex.toScimError()

            error.status shouldBe "400"
            error.scimType shouldBe "invalidFilter"
            error.detail shouldBe "Bad filter expression"
            error.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:Error")
        }
    }
}
