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
package com.marcosbarbero.scim2.core.patch

import com.fasterxml.jackson.annotation.JsonInclude
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.error.InvalidPathException
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class PatchEngineTest {

    private val faker = Faker()

    private val objectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion { incl ->
            incl.withValueInclusion(JsonInclude.Include.NON_NULL)
        }
        .build()

    private val engine = PatchEngine(objectMapper)

    private fun baseUser(): User {
        val userName = faker.name.firstName().lowercase()
        val displayName = faker.name.name()
        val workEmail = faker.internet.email()
        val homeEmail = faker.internet.email()
        return User(
            id = java.util.UUID.randomUUID().toString(),
            userName = userName,
            displayName = displayName,
            emails = listOf(
                MultiValuedAttribute(value = workEmail, type = "work", primary = true),
                MultiValuedAttribute(value = homeEmail, type = "home"),
            ),
        )
    }

    @Nested
    inner class AddOperations {

        @Test
        fun `should add simple attribute`() {
            val userName = faker.name.firstName().lowercase()
            val displayName = faker.name.name()
            val user = User(userName = userName)
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.ADD, path = "displayName", value = objectMapper.valueToTree(displayName)),
                ),
            )

            val result = engine.apply(user, request)
            result.displayName shouldBe displayName
        }

        @Test
        fun `should add to multi-valued attribute`() {
            val user = baseUser()
            val newEmail = mapOf("value" to faker.internet.email(), "type" to "other")
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.ADD, path = "emails", value = objectMapper.valueToTree(listOf(newEmail))),
                ),
            )

            val result = engine.apply(user, request)
            result.emails shouldHaveSize 3
        }

        @Test
        fun `should add with no path by merging object`() {
            val userName = faker.name.firstName().lowercase()
            val displayName = faker.name.name()
            val title = faker.name.name()
            val user = User(userName = userName)
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.ADD, value = objectMapper.valueToTree(mapOf("displayName" to displayName, "title" to title))),
                ),
            )

            val result = engine.apply(user, request)
            result.displayName shouldBe displayName
            result.title shouldBe title
        }

        @Test
        fun `should throw when add with no path has non-object value`() {
            val user = User(userName = faker.name.firstName().lowercase())
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.ADD, value = objectMapper.valueToTree("plain string")),
                ),
            )

            shouldThrow<InvalidValueException> {
                engine.apply(user, request)
            }
        }
    }

    @Nested
    inner class RemoveOperations {

        @Test
        fun `should remove simple attribute`() {
            val user = baseUser()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REMOVE, path = "displayName"),
                ),
            )

            val result = engine.apply(user, request)
            result.displayName.shouldBeNull()
        }

        @Test
        fun `should remove from multi-valued with filter`() {
            val user = baseUser()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REMOVE, path = "emails[type eq \"work\"]"),
                ),
            )

            val result = engine.apply(user, request)
            result.emails shouldHaveSize 1
            result.emails[0].type shouldBe "home"
        }

        @Test
        fun `should throw when remove has no path`() {
            val user = baseUser()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REMOVE),
                ),
            )

            shouldThrow<InvalidPathException> {
                engine.apply(user, request)
            }
        }
    }

    @Nested
    inner class ReplaceOperations {

        @Test
        fun `should replace simple attribute`() {
            val user = baseUser()
            val newDisplayName = faker.name.name()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree(newDisplayName)),
                ),
            )

            val result = engine.apply(user, request)
            result.displayName shouldBe newDisplayName
        }

        @Test
        fun `should replace multi-valued attribute entirely`() {
            val user = baseUser()
            val newEmail = faker.internet.email()
            val newEmails = listOf(mapOf("value" to newEmail, "type" to "work"))
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REPLACE, path = "emails", value = objectMapper.valueToTree(newEmails)),
                ),
            )

            val result = engine.apply(user, request)
            result.emails shouldHaveSize 1
            result.emails[0].value shouldBe newEmail
        }

        @Test
        fun `should throw when replace with no path has non-object value`() {
            val user = User(userName = faker.name.firstName().lowercase())
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REPLACE, value = objectMapper.valueToTree("plain string")),
                ),
            )

            shouldThrow<InvalidValueException> {
                engine.apply(user, request)
            }
        }
    }

    @Nested
    inner class MultipleOperations {

        @Test
        fun `should apply multiple operations in sequence`() {
            val userName = faker.name.firstName().lowercase()
            val displayName1 = faker.name.name()
            val title = faker.name.name()
            val displayName2 = faker.name.name()
            val user = User(userName = userName)
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.ADD, path = "displayName", value = objectMapper.valueToTree(displayName1)),
                    PatchOperation(op = PatchOp.ADD, path = "title", value = objectMapper.valueToTree(title)),
                    PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree(displayName2)),
                ),
            )

            val result = engine.apply(user, request)
            result.displayName shouldBe displayName2
            result.title shouldBe title
        }
    }

    @Nested
    inner class MutabilityEnforcement {

        @Test
        fun `should reject PATCH on readOnly attribute id`() {
            val user = baseUser()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REPLACE, path = "id", value = objectMapper.valueToTree("new-id")),
                ),
            )

            shouldThrow<com.marcosbarbero.scim2.core.domain.model.error.MutabilityException> {
                engine.apply(user, request)
            }
        }

        @Test
        fun `should reject PATCH on readOnly attribute meta`() {
            val user = baseUser()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REPLACE, path = "meta", value = objectMapper.valueToTree(mapOf("resourceType" to "Hacked"))),
                ),
            )

            shouldThrow<com.marcosbarbero.scim2.core.domain.model.error.MutabilityException> {
                engine.apply(user, request)
            }
        }

        @Test
        fun `should allow PATCH on readWrite attribute displayName`() {
            val user = baseUser()
            val newDisplayName = faker.name.name()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree(newDisplayName)),
                ),
            )

            val result = engine.apply(user, request)
            result.displayName shouldBe newDisplayName
        }
    }

    @Nested
    inner class PreservesIdentity {

        @Test
        fun `should preserve schemas, id, and userName`() {
            val user = baseUser()
            val title = faker.name.name()
            val request = PatchRequest(
                operations = listOf(
                    PatchOperation(op = PatchOp.ADD, path = "title", value = objectMapper.valueToTree(title)),
                ),
            )

            val result = engine.apply(user, request)
            result.id shouldBe user.id
            result.userName shouldBe user.userName
            result.schemas shouldBe user.schemas
        }
    }
}
