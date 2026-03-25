package com.marcosbarbero.scim2.core.domain.vo

import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ValueObjectsTest {

    private val faker = Faker()

    @Nested
    inner class SchemaUriTest {

        @Test
        fun `should create SchemaUri with valid URN`() {
            val urn = "urn:ietf:params:scim:schemas:core:2.0:${faker.name.name()}"
            val uri = SchemaUri(urn)
            uri.value shouldBe urn
        }

        @Test
        fun `should reject SchemaUri not starting with urn`() {
            shouldThrow<IllegalArgumentException> {
                SchemaUri("http://${faker.internet.domain()}/schema")
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
            val urn = "urn:ietf:params:scim:schemas:core:2.0:User"
            val uri = SchemaUri(urn)
            uri.toString() shouldBe urn
        }
    }

    @Nested
    inner class ResourceIdTest {

        @Test
        fun `should create ResourceId with valid value`() {
            val idValue = java.util.UUID.randomUUID().toString()
            val id = ResourceId(idValue)
            id.value shouldBe idValue
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
            val idValue = java.util.UUID.randomUUID().toString()
            val id = ResourceId(idValue)
            id.toString() shouldBe idValue
        }
    }

    @Nested
    inner class ETagTest {

        @Test
        fun `should create ETag with value`() {
            val etagValue = "W/\"${java.util.UUID.randomUUID().toString()}\""
            val etag = ETag(etagValue)
            etag.value shouldBe etagValue
        }

        @Test
        fun `toString returns the value`() {
            val etagValue = "W/\"${java.util.UUID.randomUUID().toString()}\""
            val etag = ETag(etagValue)
            etag.toString() shouldBe etagValue
        }
    }
}
