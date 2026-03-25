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
package com.marcosbarbero.scim2.core.attribute

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
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

    private val objectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion { incl ->
            incl.withValueInclusion(JsonInclude.Include.NON_NULL)
        }
        .build()

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
