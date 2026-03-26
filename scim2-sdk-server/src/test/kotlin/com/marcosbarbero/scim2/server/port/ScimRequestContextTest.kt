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
package com.marcosbarbero.scim2.server.port

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class ScimRequestContextTest {

    private val faker = Faker()

    @Test
    fun `should have defaults`() {
        val context = ScimRequestContext()

        context.principalId.shouldBeNull()
        context.roles shouldBe emptySet()
        context.attributes shouldBe emptyMap()
        context.requestedAttributes shouldBe emptyList()
        context.excludedAttributes shouldBe emptyList()
        context.correlationId.shouldNotBeEmpty()
    }

    @Test
    fun `should carry all properties`() {
        val principalId = faker.name.firstName()
        val role = faker.name.firstName().lowercase()
        val correlationId = java.util.UUID.randomUUID().toString()

        val context = ScimRequestContext(
            principalId = principalId,
            roles = setOf(role),
            attributes = mapOf("tenantId" to "t1"),
            requestedAttributes = listOf("userName"),
            excludedAttributes = listOf("password"),
            correlationId = correlationId,
        )

        context.principalId shouldBe principalId
        context.roles shouldBe setOf(role)
        context.attributes shouldBe mapOf("tenantId" to "t1")
        context.requestedAttributes shouldBe listOf("userName")
        context.excludedAttributes shouldBe listOf("password")
        context.correlationId shouldBe correlationId
    }

    @Test
    fun `copy should allow overriding fields`() {
        val original = ScimRequestContext(principalId = "user1")
        val copied = original.copy(
            requestedAttributes = listOf("userName"),
            excludedAttributes = listOf("password"),
        )

        copied.principalId shouldBe "user1"
        copied.requestedAttributes shouldBe listOf("userName")
        copied.excludedAttributes shouldBe listOf("password")
    }

    @Test
    fun `data class equality`() {
        val id = java.util.UUID.randomUUID().toString()
        val a = ScimRequestContext(principalId = "user1", correlationId = id)
        val b = ScimRequestContext(principalId = "user1", correlationId = id)

        a shouldBe b
    }

    @Test
    fun `data class inequality`() {
        val a = ScimRequestContext(principalId = "user1", correlationId = "a")
        val b = ScimRequestContext(principalId = "user2", correlationId = "a")

        a shouldNotBe b
    }
}
