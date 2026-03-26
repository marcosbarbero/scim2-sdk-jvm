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
package com.marcosbarbero.scim2.test.handler

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemoryResourceHandlerTest {

    private val faker = Faker()
    private val context = ScimRequestContext()

    private val repository = InMemoryResourceRepository<User> { user, id, meta ->
        user.copy(id = id, meta = meta)
    }

    private val patchApplier: (User, PatchRequest) -> User = { user, request ->
        var result = user
        for (op in request.operations) {
            if (op.path == "displayName" && op.op == PatchOp.REPLACE) {
                result = result.copy(displayName = op.value?.stringValue())
            }
        }
        result
    }

    private val handler = InMemoryResourceHandler(
        resourceType = User::class.java,
        endpoint = "/Users",
        repository = repository,
        patchApplier = patchApplier,
    )

    @BeforeEach
    fun setUp() {
        repository.clear()
    }

    @Test
    fun `create stores and returns resource with id`() {
        val userName = faker.name.firstName()
        val user = User(userName = userName)

        val created = handler.create(user, context)

        created.id.shouldNotBeNull()
        created.userName shouldBe userName
    }

    @Test
    fun `get returns existing resource`() {
        val created = handler.create(User(userName = faker.name.firstName()), context)

        val result = handler.get(created.id!!, context)

        result.id shouldBe created.id
    }

    @Test
    fun `get throws ResourceNotFoundException for non-existent id`() {
        shouldThrow<ResourceNotFoundException> {
            handler.get("nonexistent", context)
        }
    }

    @Test
    fun `replace updates existing resource`() {
        val created = handler.create(User(userName = faker.name.firstName()), context)
        val newUserName = faker.name.firstName()

        val replaced = handler.replace(created.id!!, User(userName = newUserName), null, context)

        replaced.id shouldBe created.id
        replaced.userName shouldBe newUserName
    }

    @Test
    fun `patch applies patchApplier and returns updated resource`() {
        val created = handler.create(User(userName = faker.name.firstName()), context)
        val newDisplayName = faker.name.lastName()
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(
                    op = PatchOp.REPLACE,
                    path = "displayName",
                    value = tools.jackson.module.kotlin.jacksonObjectMapper().valueToTree(newDisplayName),
                ),
            ),
        )

        val patched = handler.patch(created.id!!, patchRequest, null, context)

        patched.displayName shouldBe newDisplayName
    }

    @Test
    fun `patch without patchApplier returns existing resource unchanged`() {
        val handlerWithoutPatch = InMemoryResourceHandler(
            resourceType = User::class.java,
            endpoint = "/Users",
            repository = repository,
        )
        val created = handlerWithoutPatch.create(User(userName = faker.name.firstName()), context)
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = null),
            ),
        )

        val patched = handlerWithoutPatch.patch(created.id!!, patchRequest, null, context)

        patched.userName shouldBe created.userName
    }

    @Test
    fun `patch throws ResourceNotFoundException for non-existent id`() {
        val patchRequest = PatchRequest(operations = emptyList())

        shouldThrow<ResourceNotFoundException> {
            handler.patch("nonexistent", patchRequest, null, context)
        }
    }

    @Test
    fun `delete removes resource`() {
        val created = handler.create(User(userName = faker.name.firstName()), context)

        handler.delete(created.id!!, null, context)

        shouldThrow<ResourceNotFoundException> {
            handler.get(created.id!!, context)
        }
    }

    @Test
    fun `search returns all resources`() {
        repeat(3) { handler.create(User(userName = faker.name.firstName()), context) }

        val result = handler.search(SearchRequest(), context)

        result.totalResults shouldBe 3
    }

    @Test
    fun `resourceType and endpoint are correct`() {
        handler.resourceType shouldBe User::class.java
        handler.endpoint shouldBe "/Users"
    }
}
