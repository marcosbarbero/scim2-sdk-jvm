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
package com.marcosbarbero.scim2.spring.handler

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class DefaultResourceHandlerTest {

    private val faker = Faker()
    private lateinit var repository: ResourceRepository<User>
    private lateinit var handler: DefaultResourceHandler<User>
    private lateinit var handlerWithMapper: DefaultResourceHandler<User>
    private val context = ScimRequestContext()
    private val objectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        handler = DefaultResourceHandler(
            resourceType = User::class.java,
            endpoint = "/Users",
            repository = repository,
        )
        handlerWithMapper = DefaultResourceHandler(
            resourceType = User::class.java,
            endpoint = "/Users",
            repository = repository,
            objectMapper = objectMapper,
        )
    }

    @Test
    fun `get returns resource from repository`() {
        val userId = faker.random.randomString(10)
        val userName = faker.name.firstName()
        val user = User(id = userId, userName = userName)
        every { repository.findById(userId) } returns user

        val result = handler.get(userId, context)

        result shouldBe user
        verify { repository.findById(userId) }
    }

    @Test
    fun `get throws ResourceNotFoundException when resource not found`() {
        val userId = faker.random.randomString(10)
        every { repository.findById(userId) } returns null

        shouldThrow<ResourceNotFoundException> {
            handler.get(userId, context)
        }
    }

    @Test
    fun `create delegates to repository`() {
        val userName = faker.name.firstName()
        val user = User(userName = userName)
        val createdUser = User(id = faker.random.randomString(10), userName = userName)
        every { repository.create(user) } returns createdUser

        val result = handler.create(user, context)

        result shouldBe createdUser
        verify { repository.create(user) }
    }

    @Test
    fun `replace delegates to repository`() {
        val userId = faker.random.randomString(10)
        val userName = faker.name.firstName()
        val user = User(userName = userName)
        val version = faker.random.randomString(5)
        val replacedUser = User(id = userId, userName = userName)
        every { repository.replace(userId, user, version) } returns replacedUser

        val result = handler.replace(userId, user, version, context)

        result shouldBe replacedUser
        verify { repository.replace(userId, user, version) }
    }

    @Test
    fun `patch applies operations via PatchEngine and replaces`() {
        val userId = faker.random.randomString(10)
        val userName = faker.name.firstName()
        val newDisplayName = faker.name.name()
        val existingUser = User(id = userId, userName = userName)
        val version = faker.random.randomString(5)
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree(newDisplayName)),
            ),
        )
        every { repository.findById(userId) } returns existingUser
        every { repository.replace(eq(userId), any(), eq(version)) } answers { secondArg() }

        val result = handlerWithMapper.patch(userId, patchRequest, version, context)

        result.displayName shouldBe newDisplayName
        verify { repository.findById(userId) }
        verify { repository.replace(userId, any(), version) }
    }

    @Test
    fun `patch throws UnsupportedOperationException when no ObjectMapper configured`() {
        val userId = faker.random.randomString(10)
        val existingUser = User(id = userId, userName = faker.name.firstName())
        val patchRequest = PatchRequest(operations = emptyList())
        every { repository.findById(userId) } returns existingUser

        shouldThrow<UnsupportedOperationException> {
            handler.patch(userId, patchRequest, null, context)
        }
    }

    @Test
    fun `patch throws ResourceNotFoundException when resource not found`() {
        val userId = faker.random.randomString(10)
        val patchRequest = PatchRequest(operations = emptyList())
        every { repository.findById(userId) } returns null

        shouldThrow<ResourceNotFoundException> {
            handlerWithMapper.patch(userId, patchRequest, null, context)
        }
    }

    @Test
    fun `delete verifies existence then delegates to repository`() {
        val userId = faker.random.randomString(10)
        val version = faker.random.randomString(5)
        val existingUser = User(id = userId, userName = faker.name.firstName())
        every { repository.findById(userId) } returns existingUser

        handler.delete(userId, version, context)

        verify { repository.findById(userId) }
        verify { repository.delete(userId, version) }
    }

    @Test
    fun `delete throws ResourceNotFoundException when resource not found`() {
        val userId = faker.random.randomString(10)
        every { repository.findById(userId) } returns null

        shouldThrow<ResourceNotFoundException> {
            handler.delete(userId, null, context)
        }
    }

    @Test
    fun `search delegates to repository`() {
        val searchRequest = SearchRequest(filter = "userName eq \"${faker.name.firstName()}\"")
        val listResponse = ListResponse<User>(totalResults = 0, resources = emptyList())
        every { repository.search(searchRequest) } returns listResponse

        val result = handler.search(searchRequest, context)

        result shouldBe listResponse
        verify { repository.search(searchRequest) }
    }

    @Test
    fun `resourceType returns configured type`() {
        handler.resourceType shouldBe User::class.java
    }

    @Test
    fun `endpoint returns configured endpoint`() {
        handler.endpoint shouldBe "/Users"
    }
}
