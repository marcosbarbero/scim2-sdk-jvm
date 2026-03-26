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

import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.Schema
import com.marcosbarbero.scim2.core.domain.model.schema.ServiceProviderConfig
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncScimClientTest {

    private val faker = Faker()
    private lateinit var delegate: ScimClient
    private lateinit var asyncClient: AsyncScimClient

    @BeforeEach
    fun setUp() {
        delegate = mockk(relaxed = true)
        asyncClient = AsyncScimClient(delegate)
    }

    @Test
    fun `create delegates to ScimClient via Dispatchers IO`() = runTest {
        val userName = faker.name.firstName()
        val user = User(userName = userName)
        val expected = ScimResponse(value = user, statusCode = 201)
        every { delegate.create("/Users", user, User::class) } returns expected

        val result = asyncClient.create("/Users", user, User::class)

        result shouldBe expected
        verify { delegate.create("/Users", user, User::class) }
    }

    @Test
    fun `get delegates to ScimClient via Dispatchers IO`() = runTest {
        val userId = faker.random.randomString(10)
        val user = User(id = userId, userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 200)
        every { delegate.get("/Users", userId, User::class) } returns expected

        val result = asyncClient.get("/Users", userId, User::class)

        result shouldBe expected
        verify { delegate.get("/Users", userId, User::class) }
    }

    @Test
    fun `replace delegates to ScimClient via Dispatchers IO`() = runTest {
        val userId = faker.random.randomString(10)
        val user = User(id = userId, userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 200)
        every { delegate.replace("/Users", userId, user, User::class) } returns expected

        val result = asyncClient.replace("/Users", userId, user, User::class)

        result shouldBe expected
        verify { delegate.replace("/Users", userId, user, User::class) }
    }

    @Test
    fun `patch delegates to ScimClient via Dispatchers IO`() = runTest {
        val userId = faker.random.randomString(10)
        val patchRequest = PatchRequest()
        val user = User(id = userId, userName = faker.name.firstName())
        val expected = ScimResponse(value = user, statusCode = 200)
        every { delegate.patch("/Users", userId, patchRequest, User::class) } returns expected

        val result = asyncClient.patch("/Users", userId, patchRequest, User::class)

        result shouldBe expected
        verify { delegate.patch("/Users", userId, patchRequest, User::class) }
    }

    @Test
    fun `delete delegates to ScimClient via Dispatchers IO`() = runTest {
        val userId = faker.random.randomString(10)

        asyncClient.delete("/Users", userId)

        verify { delegate.delete("/Users", userId) }
    }

    @Test
    fun `search delegates to ScimClient via Dispatchers IO`() = runTest {
        val searchRequest = SearchRequest(filter = "userName eq \"${faker.name.firstName()}\"")
        val listResponse = ListResponse<User>(totalResults = 0, resources = emptyList())
        val expected = ScimResponse(value = listResponse, statusCode = 200)
        every { delegate.search("/Users", searchRequest, User::class) } returns expected

        val result = asyncClient.search("/Users", searchRequest, User::class)

        result shouldBe expected
        verify { delegate.search("/Users", searchRequest, User::class) }
    }

    @Test
    fun `bulk delegates to ScimClient via Dispatchers IO`() = runTest {
        val bulkRequest = BulkRequest(operations = emptyList())
        val bulkResponse = BulkResponse(operations = emptyList())
        val expected = ScimResponse(value = bulkResponse, statusCode = 200)
        every { delegate.bulk(bulkRequest) } returns expected

        val result = asyncClient.bulk(bulkRequest)

        result shouldBe expected
        verify { delegate.bulk(bulkRequest) }
    }

    @Test
    fun `getServiceProviderConfig delegates to ScimClient via Dispatchers IO`() = runTest {
        val config = ServiceProviderConfig()
        val expected = ScimResponse(value = config, statusCode = 200)
        every { delegate.getServiceProviderConfig() } returns expected

        val result = asyncClient.getServiceProviderConfig()

        result shouldBe expected
        verify { delegate.getServiceProviderConfig() }
    }

    @Test
    fun `getSchemas delegates to ScimClient via Dispatchers IO`() = runTest {
        val listResponse = ListResponse<Schema>(totalResults = 0, resources = emptyList())
        val expected = ScimResponse(value = listResponse, statusCode = 200)
        every { delegate.getSchemas() } returns expected

        val result = asyncClient.getSchemas()

        result shouldBe expected
        verify { delegate.getSchemas() }
    }

    @Test
    fun `getResourceTypes delegates to ScimClient via Dispatchers IO`() = runTest {
        val listResponse = ListResponse<ResourceType>(totalResults = 0, resources = emptyList())
        val expected = ScimResponse(value = listResponse, statusCode = 200)
        every { delegate.getResourceTypes() } returns expected

        val result = asyncClient.getResourceTypes()

        result shouldBe expected
        verify { delegate.getResourceTypes() }
    }

    @Test
    fun `close delegates to ScimClient`() {
        asyncClient.close()

        verify { delegate.close() }
    }
}
