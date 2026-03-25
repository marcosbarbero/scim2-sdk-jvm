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
package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ScimClientExtensionsTest {

    private val faker = Faker()
    private lateinit var client: ScimClient

    private val userId = java.util.UUID.randomUUID().toString()
    private val groupId = java.util.UUID.randomUUID().toString()
    private val userName = "john_${System.nanoTime()}"
    private val groupName = "admins_${System.nanoTime()}"

    private val userResponse = ScimResponse(
        value = User(id = userId, userName = userName),
        statusCode = 200
    )
    private val groupResponse = ScimResponse(
        value = Group(id = groupId, displayName = groupName),
        statusCode = 200
    )

    @BeforeEach
    fun setUp() {
        client = mockk(relaxed = true)
    }

    // ===== User Operations =====

    @Test
    fun `createUser calls create with Users endpoint and User class`() {
        val user = User(userName = userName)
        val expected = ScimResponse(value = User(id = userId, userName = userName), statusCode = 201)
        every { client.create("/Users", user, User::class) } returns expected

        val result = client.createUser(user)

        result shouldBe expected
        verify { client.create("/Users", user, User::class) }
    }

    @Test
    fun `getUser calls get with Users endpoint and User class`() {
        every { client.get("/Users", userId, User::class) } returns userResponse

        val result = client.getUser(userId)

        result shouldBe userResponse
        verify { client.get("/Users", userId, User::class) }
    }

    @Test
    fun `replaceUser calls replace with Users endpoint and User class`() {
        val updatedName = faker.name.firstName().lowercase()
        val user = User(id = userId, userName = updatedName)
        every { client.replace("/Users", userId, user, User::class) } returns userResponse

        client.replaceUser(userId, user)

        verify { client.replace("/Users", userId, user, User::class) }
    }

    @Test
    fun `patchUser calls patch with Users endpoint and User class`() {
        val patch = PatchRequest()
        every { client.patch("/Users", userId, patch, User::class) } returns userResponse

        client.patchUser(userId, patch)

        verify { client.patch("/Users", userId, patch, User::class) }
    }

    @Test
    fun `deleteUser calls delete with Users endpoint`() {
        client.deleteUser(userId)

        verify { client.delete("/Users", userId) }
    }

    @Test
    fun `searchUsers with default request calls search with Users endpoint`() {
        val listResponse = ScimResponse(
            value = ListResponse<User>(totalResults = 0),
            statusCode = 200
        )
        every { client.search("/Users", any<SearchRequest>(), User::class) } returns listResponse

        client.searchUsers()

        verify { client.search("/Users", any<SearchRequest>(), User::class) }
    }

    @Test
    fun `searchUsers with filter string builds SearchRequest`() {
        val searchName = faker.name.firstName().lowercase()
        val listResponse = ScimResponse(
            value = ListResponse<User>(totalResults = 1, resources = listOf(User(userName = searchName))),
            statusCode = 200
        )
        val requestSlot = slot<SearchRequest>()
        every { client.search("/Users", capture(requestSlot), User::class) } returns listResponse

        client.searchUsers("userName sw \"$searchName\"")

        requestSlot.captured.filter shouldBe "userName sw \"$searchName\""
    }

    // ===== Group Operations =====

    @Test
    fun `createGroup calls create with Groups endpoint and Group class`() {
        val group = Group(displayName = groupName)
        val expected = ScimResponse(value = Group(id = groupId, displayName = groupName), statusCode = 201)
        every { client.create("/Groups", group, Group::class) } returns expected

        val result = client.createGroup(group)

        result shouldBe expected
        verify { client.create("/Groups", group, Group::class) }
    }

    @Test
    fun `getGroup calls get with Groups endpoint and Group class`() {
        every { client.get("/Groups", groupId, Group::class) } returns groupResponse

        val result = client.getGroup(groupId)

        result shouldBe groupResponse
        verify { client.get("/Groups", groupId, Group::class) }
    }

    @Test
    fun `replaceGroup calls replace with Groups endpoint and Group class`() {
        val updatedGroupName = faker.name.name()
        val group = Group(id = groupId, displayName = updatedGroupName)
        every { client.replace("/Groups", groupId, group, Group::class) } returns groupResponse

        client.replaceGroup(groupId, group)

        verify { client.replace("/Groups", groupId, group, Group::class) }
    }

    @Test
    fun `patchGroup calls patch with Groups endpoint and Group class`() {
        val patch = PatchRequest()
        every { client.patch("/Groups", groupId, patch, Group::class) } returns groupResponse

        client.patchGroup(groupId, patch)

        verify { client.patch("/Groups", groupId, patch, Group::class) }
    }

    @Test
    fun `deleteGroup calls delete with Groups endpoint`() {
        client.deleteGroup(groupId)

        verify { client.delete("/Groups", groupId) }
    }

    @Test
    fun `searchGroups calls search with Groups endpoint`() {
        val listResponse = ScimResponse(
            value = ListResponse<Group>(totalResults = 0),
            statusCode = 200
        )
        every { client.search("/Groups", any<SearchRequest>(), Group::class) } returns listResponse

        client.searchGroups()

        verify { client.search("/Groups", any<SearchRequest>(), Group::class) }
    }

    // ===== Generic typed operations =====

    @Test
    fun `createResource reads ScimResource annotation for User`() {
        val user = User(userName = userName)
        val expected = ScimResponse(value = User(id = userId, userName = userName), statusCode = 201)
        every { client.create("/Users", user, User::class) } returns expected

        val result = client.createResource(user)

        result shouldBe expected
        verify { client.create("/Users", user, User::class) }
    }

    @Test
    fun `createResource reads ScimResource annotation for Group`() {
        val group = Group(displayName = groupName)
        val expected = ScimResponse(value = Group(id = groupId, displayName = groupName), statusCode = 201)
        every { client.create("/Groups", group, Group::class) } returns expected

        val result = client.createResource(group)

        result shouldBe expected
        verify { client.create("/Groups", group, Group::class) }
    }

    @Test
    fun `createResource throws for unannotated class`() {
        val unannotated = object : ScimResource(schemas = listOf("urn:test")) {}

        val exception = shouldThrow<IllegalArgumentException> {
            client.createResource(unannotated)
        }

        exception.message shouldContain "is not annotated with @ScimResource"
    }

    @Test
    fun `getResource reads ScimResource annotation`() {
        every { client.get("/Users", userId, User::class) } returns userResponse

        val result = client.getResource<User>(userId)

        result shouldBe userResponse
        verify { client.get("/Users", userId, User::class) }
    }

    @Test
    fun `searchResources reads ScimResource annotation`() {
        val listResponse = ScimResponse(
            value = ListResponse<User>(totalResults = 0),
            statusCode = 200
        )
        every { client.search("/Users", any<SearchRequest>(), User::class) } returns listResponse

        client.searchResources<User>()

        verify { client.search("/Users", any<SearchRequest>(), User::class) }
    }
}
