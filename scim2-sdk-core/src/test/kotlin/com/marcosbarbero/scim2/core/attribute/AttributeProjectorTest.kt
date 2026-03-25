package com.marcosbarbero.scim2.core.attribute

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AttributeProjectorTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    private val projector = AttributeProjector(objectMapper)

    private fun fullUser() = User(
        id = "123",
        userName = "bjensen",
        displayName = "Babs Jensen",
        title = "Tour Guide",
        nickName = "Babs",
        active = true
    )

    @Nested
    inner class IncludeAttributes {

        @Test
        fun `should include only specified attributes`() {
            val result = projector.project(fullUser(), attributes = listOf("userName", "displayName"))
            result.userName shouldBe "bjensen"
            result.displayName shouldBe "Babs Jensen"
            result.title.shouldBeNull()
            result.nickName.shouldBeNull()
            result.active.shouldBeNull()
        }

        @Test
        fun `should always include id and schemas`() {
            val result = projector.project(fullUser(), attributes = listOf("userName"))
            result.id shouldBe "123"
            result.schemas.shouldNotBeNull()
            result.userName shouldBe "bjensen"
            result.displayName.shouldBeNull()
        }
    }

    @Nested
    inner class ExcludeAttributes {

        @Test
        fun `should exclude specified attributes`() {
            val result = projector.project(fullUser(), excludedAttributes = listOf("title", "nickName"))
            result.userName shouldBe "bjensen"
            result.displayName shouldBe "Babs Jensen"
            result.title.shouldBeNull()
            result.nickName.shouldBeNull()
            result.active shouldBe true
        }

        @Test
        fun `should not exclude always-returned attributes`() {
            val result = projector.project(fullUser(), excludedAttributes = listOf("id", "schemas"))
            result.id shouldBe "123"
            result.schemas.shouldNotBeNull()
        }
    }

    @Nested
    inner class ErrorCases {

        @Test
        fun `should throw when both attributes and excludedAttributes provided`() {
            shouldThrow<InvalidValueException> {
                projector.project(
                    fullUser(),
                    attributes = listOf("userName"),
                    excludedAttributes = listOf("title")
                )
            }
        }
    }

    @Nested
    inner class NoProjection {

        @Test
        fun `should return full resource when no attributes specified`() {
            val result = projector.project(fullUser())
            result.userName shouldBe "bjensen"
            result.displayName shouldBe "Babs Jensen"
            result.title shouldBe "Tour Guide"
            result.nickName shouldBe "Babs"
            result.active shouldBe true
        }
    }
}
