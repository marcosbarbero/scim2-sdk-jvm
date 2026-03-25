package com.marcosbarbero.scim2.core.attribute

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class AttributeProjectorTest {

    private val faker = Faker()

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    private val projector = AttributeProjector(objectMapper)

    private val userId = java.util.UUID.randomUUID().toString()
    private val userName = faker.name.firstName().lowercase()
    private val displayName = faker.name.name()
    private val title = faker.name.name()
    private val nickName = faker.name.firstName()

    private fun fullUser() = User(
        id = userId,
        userName = userName,
        displayName = displayName,
        title = title,
        nickName = nickName,
        active = true
    )

    @Nested
    inner class IncludeAttributes {

        @Test
        fun `should include only specified attributes`() {
            val result = projector.project(fullUser(), attributes = listOf("userName", "displayName"))
            result.userName shouldBe userName
            result.displayName shouldBe displayName
            result.title.shouldBeNull()
            result.nickName.shouldBeNull()
            result.active.shouldBeNull()
        }

        @Test
        fun `should always include id and schemas`() {
            val result = projector.project(fullUser(), attributes = listOf("userName"))
            result.id shouldBe userId
            result.schemas.shouldNotBeNull()
            result.userName shouldBe userName
            result.displayName.shouldBeNull()
        }

        @Test
        fun `should always include meta when projecting`() {
            val now = Instant.now()
            val user = fullUser().copy(meta = Meta(created = now, lastModified = now, resourceType = "User"))
            val result = projector.project(user, attributes = listOf("userName"))
            result.meta.shouldNotBeNull()
            result.meta!!.resourceType shouldBe "User"
        }
    }

    @Nested
    inner class ExcludeAttributes {

        @Test
        fun `should exclude specified attributes`() {
            val result = projector.project(fullUser(), excludedAttributes = listOf("title", "nickName"))
            result.userName shouldBe userName
            result.displayName shouldBe displayName
            result.title.shouldBeNull()
            result.nickName.shouldBeNull()
            result.active shouldBe true
        }

        @Test
        fun `should not exclude always-returned attributes`() {
            val result = projector.project(fullUser(), excludedAttributes = listOf("id", "schemas"))
            result.id shouldBe userId
            result.schemas.shouldNotBeNull()
        }

        @Test
        fun `should not exclude meta`() {
            val now = Instant.now()
            val user = fullUser().copy(meta = Meta(created = now, lastModified = now, resourceType = "User"))
            val result = projector.project(user, excludedAttributes = listOf("meta"))
            result.meta.shouldNotBeNull()
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
            result.userName shouldBe userName
            result.displayName shouldBe displayName
            result.title shouldBe title
            result.nickName shouldBe nickName
            result.active shouldBe true
        }
    }
}
