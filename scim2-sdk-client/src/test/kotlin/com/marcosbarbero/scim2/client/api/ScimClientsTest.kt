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
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScimClientsTest {

    private val faker = Faker()
    private lateinit var client: ScimClient

    @BeforeEach
    fun setUp() {
        client = mockk(relaxed = true)
    }

    // --- User Operations ---

    @Test
    fun `createUser delegates to extension function`() {
        val user = User(userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 201)
        every { client.create("/Users", user, User::class) } returns expected

        val result = ScimClients.createUser(client, user)

        result shouldBe expected
        verify { client.create("/Users", user, User::class) }
    }

    @Test
    fun `getUser delegates to extension function`() {
        val userId = faker.random.randomString(10)
        val user = User(id = userId, userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 200)
        every { client.get("/Users", userId, User::class) } returns expected

        val result = ScimClients.getUser(client, userId)

        result shouldBe expected
        verify { client.get("/Users", userId, User::class) }
    }

    @Test
    fun `replaceUser delegates to extension function`() {
        val userId = faker.random.randomString(10)
        val user = User(id = userId, userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 200)
        every { client.replace("/Users", userId, user, User::class) } returns expected

        val result = ScimClients.replaceUser(client, userId, user)

        result shouldBe expected
        verify { client.replace("/Users", userId, user, User::class) }
    }

    @Test
    fun `patchUser delegates to extension function`() {
        val userId = faker.random.randomString(10)
        val patchRequest = PatchRequest()
        val user = User(id = userId, userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 200)
        every { client.patch("/Users", userId, patchRequest, User::class) } returns expected

        val result = ScimClients.patchUser(client, userId, patchRequest)

        result shouldBe expected
        verify { client.patch("/Users", userId, patchRequest, User::class) }
    }

    @Test
    fun `deleteUser delegates to extension function`() {
        val userId = faker.random.randomString(10)

        ScimClients.deleteUser(client, userId)

        verify { client.delete("/Users", userId) }
    }

    @Test
    fun `searchUsers with SearchRequest delegates to extension function`() {
        val searchRequest = SearchRequest(
            filter = "userName eq \"${faker.name.firstName()}\"",
        )
        val listResponse = ListResponse<User>(totalResults = 0, resources = emptyList())
        val expected = ScimResponse(value = listResponse, statusCode = 200)
        every { client.search("/Users", searchRequest, User::class) } returns expected

        val result = ScimClients.searchUsers(client, searchRequest)

        result shouldBe expected
        verify { client.search("/Users", searchRequest, User::class) }
    }

    @Test
    fun `searchUsers with filter string delegates to extension function`() {
        val filter = "userName eq \"${faker.name.firstName()}\""
        val listResponse = ListResponse<User>(totalResults = 0, resources = emptyList())
        val expected = ScimResponse(value = listResponse, statusCode = 200)
        every {
            client.search(eq("/Users"), any<SearchRequest>(), eq(User::class))
        } returns expected

        val result = ScimClients.searchUsers(client, filter)

        result shouldBe expected
        verify {
            client.search(
                eq("/Users"),
                match<SearchRequest> { it.filter == filter },
                eq(User::class),
            )
        }
    }

    // --- Group Operations ---

    @Test
    fun `createGroup delegates to extension function`() {
        val group = Group(displayName = faker.name.lastName())
        val expected = ScimResponse(value = group, statusCode = 201)
        every { client.create("/Groups", group, Group::class) } returns expected

        val result = ScimClients.createGroup(client, group)

        result shouldBe expected
        verify { client.create("/Groups", group, Group::class) }
    }

    @Test
    fun `getGroup delegates to extension function`() {
        val groupId = faker.random.randomString(10)
        val group = Group(id = groupId, displayName = faker.name.lastName())
        val expected = ScimResponse(value = group, statusCode = 200)
        every { client.get("/Groups", groupId, Group::class) } returns expected

        val result = ScimClients.getGroup(client, groupId)

        result shouldBe expected
        verify { client.get("/Groups", groupId, Group::class) }
    }

    @Test
    fun `replaceGroup delegates to extension function`() {
        val groupId = faker.random.randomString(10)
        val group = Group(id = groupId, displayName = faker.name.lastName())
        val expected = ScimResponse(value = group, statusCode = 200)
        every { client.replace("/Groups", groupId, group, Group::class) } returns expected

        val result = ScimClients.replaceGroup(client, groupId, group)

        result shouldBe expected
        verify { client.replace("/Groups", groupId, group, Group::class) }
    }

    @Test
    fun `patchGroup delegates to extension function`() {
        val groupId = faker.random.randomString(10)
        val patchRequest = PatchRequest()
        val group = Group(id = groupId, displayName = faker.name.lastName())
        val expected = ScimResponse(value = group, statusCode = 200)
        every { client.patch("/Groups", groupId, patchRequest, Group::class) } returns expected

        val result = ScimClients.patchGroup(client, groupId, patchRequest)

        result shouldBe expected
        verify { client.patch("/Groups", groupId, patchRequest, Group::class) }
    }

    @Test
    fun `deleteGroup delegates to extension function`() {
        val groupId = faker.random.randomString(10)

        ScimClients.deleteGroup(client, groupId)

        verify { client.delete("/Groups", groupId) }
    }

    @Test
    fun `searchGroups delegates to extension function`() {
        val searchRequest = SearchRequest()
        val listResponse = ListResponse<Group>(totalResults = 0, resources = emptyList())
        val expected = ScimResponse(value = listResponse, statusCode = 200)
        every { client.search("/Groups", searchRequest, Group::class) } returns expected

        val result = ScimClients.searchGroups(client, searchRequest)

        result shouldBe expected
        verify { client.search("/Groups", searchRequest, Group::class) }
    }
}
