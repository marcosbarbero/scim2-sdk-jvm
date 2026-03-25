package com.marcosbarbero.scim2.core.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ValueObjectsTest {

    @Nested
    inner class SchemaUriTest {

        @Test
        fun `should create SchemaUri with valid URN`() {
            val uri = SchemaUri("urn:ietf:params:scim:schemas:core:2.0:User")
            uri.value shouldBe "urn:ietf:params:scim:schemas:core:2.0:User"
        }

        @Test
        fun `should reject SchemaUri not starting with urn`() {
            shouldThrow<IllegalArgumentException> {
                SchemaUri("http://example.com/schema")
            }
        }

        @Test
        fun `should reject blank SchemaUri`() {
            shouldThrow<IllegalArgumentException> {
                SchemaUri("   ")
            }
        }

        @Test
        fun `toString returns the value`() {
            val uri = SchemaUri("urn:ietf:params:scim:schemas:core:2.0:User")
            uri.toString() shouldBe "urn:ietf:params:scim:schemas:core:2.0:User"
        }
    }

    @Nested
    inner class ResourceIdTest {

        @Test
        fun `should create ResourceId with valid value`() {
            val id = ResourceId("abc-123")
            id.value shouldBe "abc-123"
        }

        @Test
        fun `should reject blank ResourceId`() {
            shouldThrow<IllegalArgumentException> {
                ResourceId("")
            }
        }

        @Test
        fun `should reject whitespace-only ResourceId`() {
            shouldThrow<IllegalArgumentException> {
                ResourceId("   ")
            }
        }

        @Test
        fun `toString returns the value`() {
            val id = ResourceId("abc-123")
            id.toString() shouldBe "abc-123"
        }
    }

    @Nested
    inner class ETagTest {

        @Test
        fun `should create ETag with value`() {
            val etag = ETag("W/\"abc\"")
            etag.value shouldBe "W/\"abc\""
        }

        @Test
        fun `toString returns the value`() {
            val etag = ETag("W/\"abc\"")
            etag.toString() shouldBe "W/\"abc\""
        }
    }
}
