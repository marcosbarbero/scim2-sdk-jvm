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

import com.marcosbarbero.scim2.client.error.ScimClientException
import com.marcosbarbero.scim2.client.port.BearerTokenAuthentication
import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.domain.model.error.ScimError
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.schema.ServiceProviderConfig
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class DefaultScimClientTest {

    private val faker = Faker()
    private lateinit var transport: HttpTransport
    private lateinit var serializer: ScimSerializer
    private lateinit var client: ScimClient

    private val baseUrl = "https://scim.example.com"

    @BeforeEach
    fun setUp() {
        transport = mockk(relaxed = true)
        serializer = mockk(relaxed = true)
        client = ScimClientBuilder()
            .baseUrl(baseUrl)
            .transport(transport)
            .serializer(serializer)
            .authentication(BearerTokenAuthentication("test-token"))
            .build()
    }

    @Test
    fun `create sends POST request and returns 201 with resource`() {
        val userName = faker.name.firstName()
        val user = User(userName = userName)
        val createdUser = User(id = "123", userName = userName)
        val serializedBody = """{"userName":"$userName"}""".toByteArray()
        val responseBody = """{"id":"123","userName":"$userName"}""".toByteArray()

        every { serializer.serialize(user) } returns serializedBody
        every { serializer.deserialize(responseBody, User::class) } returns createdUser
        every { transport.execute(any()) } returns HttpResponse(
            statusCode = 201,
            headers = mapOf(
                "ETag" to listOf("W/\"1\""),
                "Location" to listOf("$baseUrl/Users/123"),
            ),
            body = responseBody,
        )

        val response = client.create("/Users", user, User::class)

        response.statusCode shouldBe 201
        response.value shouldBe createdUser
        response.etag shouldBe "W/\"1\""
        response.location shouldBe "$baseUrl/Users/123"

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "POST"
        requestSlot.captured.url shouldBe "$baseUrl/Users"
    }

    @Test
    fun `get sends GET request and returns 200 with resource`() {
        val userId = faker.random.randomString(10)
        val userName = faker.name.firstName().lowercase()
        val user = User(id = userId, userName = userName)
        val responseBody = """{"id":"$userId"}""".toByteArray()

        every { serializer.deserialize(responseBody, User::class) } returns user
        every { transport.execute(any()) } returns HttpResponse(
            statusCode = 200,
            body = responseBody,
        )

        val response = client.get("/Users", userId, User::class)

        response.statusCode shouldBe 200
        response.value shouldBe user

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "GET"
        requestSlot.captured.url shouldBe "$baseUrl/Users/$userId"
    }

    @Test
    fun `replace sends PUT request and returns 200`() {
        val userId = faker.random.randomString(10)
        val user = User(id = userId, userName = "updated")
        val serializedBody = """{"userName":"updated"}""".toByteArray()
        val responseBody = """{"id":"$userId","userName":"updated"}""".toByteArray()

        every { serializer.serialize(user) } returns serializedBody
        every { serializer.deserialize(responseBody, User::class) } returns user
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.replace("/Users", userId, user, User::class)

        response.statusCode shouldBe 200

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "PUT"
        requestSlot.captured.url shouldBe "$baseUrl/Users/$userId"
    }

    @Test
    fun `patch sends PATCH request with PatchRequest body`() {
        val userId = faker.random.randomString(10)
        val patchRequest = PatchRequest()
        val user = User(id = userId, userName = "patched", active = true)
        val serializedBody = """{"Operations":[]}""".toByteArray()
        val responseBody = """{"id":"$userId"}""".toByteArray()

        every { serializer.serialize(patchRequest) } returns serializedBody
        every { serializer.deserialize(responseBody, User::class) } returns user
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.patch("/Users", userId, patchRequest, User::class)

        response.statusCode shouldBe 200

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "PATCH"
        requestSlot.captured.url shouldBe "$baseUrl/Users/$userId"
        requestSlot.captured.body shouldBe serializedBody
    }

    @Test
    fun `delete sends DELETE request and returns 204`() {
        val userId = faker.random.randomString(10)

        every { transport.execute(any()) } returns HttpResponse(statusCode = 204)

        client.delete("/Users", userId)

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "DELETE"
        requestSlot.captured.url shouldBe "$baseUrl/Users/$userId"
    }

    @Test
    fun `search sends POST to search endpoint`() {
        val searchUserName = faker.name.firstName().lowercase()
        val searchRequest = SearchRequest(filter = "userName eq \"$searchUserName\"", startIndex = 1, count = 10)
        val listResponse = ListResponse<User>(totalResults = 1, resources = listOf(User(userName = searchUserName)))
        val serializedBody = """{"filter":"userName eq \"$searchUserName\""}""".toByteArray()
        val responseBody = """{"totalResults":1}""".toByteArray()

        every { serializer.serialize(searchRequest) } returns serializedBody
        @Suppress("UNCHECKED_CAST")
        every { serializer.deserialize(responseBody, ListResponse::class as KClass<ListResponse<*>>) } returns listResponse
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.search("/Users", searchRequest, User::class)

        response.statusCode shouldBe 200
        response.value.totalResults shouldBe 1

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "POST"
        requestSlot.captured.url shouldBe "$baseUrl/Users/.search"
    }

    @Test
    fun `error response throws ScimClientException with ScimError`() {
        val scimError = ScimError(
            status = "404",
            detail = "Resource not found",
        )
        val errorBody = """{"status":"404","detail":"Resource not found"}""".toByteArray()

        every { serializer.deserialize(errorBody, ScimError::class) } returns scimError
        every { transport.execute(any()) } returns HttpResponse(statusCode = 404, body = errorBody)

        val exception = shouldThrow<ScimClientException> {
            client.get("/Users", "nonexistent", User::class)
        }

        exception.statusCode shouldBe 404
        exception.scimError shouldNotBe null
        exception.scimError!!.detail shouldBe "Resource not found"
    }

    @Test
    fun `error response without parseable body throws ScimClientException`() {
        every { serializer.deserialize(any<ByteArray>(), eq(ScimError::class)) } throws RuntimeException("parse error")
        every { transport.execute(any()) } returns HttpResponse(statusCode = 500, body = "Internal Error".toByteArray())

        val exception = shouldThrow<ScimClientException> {
            client.get("/Users", "123", User::class)
        }

        exception.statusCode shouldBe 500
        exception.scimError shouldBe null
    }

    @Test
    fun `authentication strategy is applied to requests`() {
        val responseBody = """{"id":"123"}""".toByteArray()
        every { serializer.deserialize(responseBody, User::class) } returns User(id = "123", userName = "test")
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        client.get("/Users", "123", User::class)

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.headers["Authorization"] shouldBe "Bearer test-token"
    }

    @Test
    fun `Content-Type header is set on requests with body`() {
        val user = User(userName = "test")
        val serializedBody = """{"userName":"test"}""".toByteArray()
        val responseBody = serializedBody

        every { serializer.serialize(user) } returns serializedBody
        every { serializer.deserialize(responseBody, User::class) } returns user
        every { transport.execute(any()) } returns HttpResponse(statusCode = 201, body = responseBody)

        client.create("/Users", user, User::class)

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.headers["Content-Type"] shouldBe "application/scim+json"
        requestSlot.captured.headers["Accept"] shouldBe "application/scim+json"
    }

    @Test
    fun `Accept header is set on GET requests without body`() {
        val responseBody = """{"id":"123"}""".toByteArray()
        every { serializer.deserialize(responseBody, User::class) } returns User(id = "123", userName = "test")
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        client.get("/Users", "123", User::class)

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.headers["Accept"] shouldBe "application/scim+json"
        requestSlot.captured.headers.containsKey("Content-Type") shouldBe false
    }

    @Test
    fun `ETag and Location are extracted from response headers`() {
        val user = User(userName = "test")
        val serializedBody = """{}""".toByteArray()
        val responseBody = serializedBody

        every { serializer.serialize(user) } returns serializedBody
        every { serializer.deserialize(responseBody, User::class) } returns user
        every { transport.execute(any()) } returns HttpResponse(
            statusCode = 201,
            headers = mapOf(
                "ETag" to listOf("W/\"abc\""),
                "Location" to listOf("https://scim.example.com/Users/456"),
            ),
            body = responseBody,
        )

        val response = client.create("/Users", user, User::class)

        response.etag shouldBe "W/\"abc\""
        response.location shouldBe "https://scim.example.com/Users/456"
    }

    @Test
    fun `getServiceProviderConfig sends GET to ServiceProviderConfig`() {
        val config = ServiceProviderConfig()
        val responseBody = """{}""".toByteArray()

        every { serializer.deserialize(responseBody, ServiceProviderConfig::class) } returns config
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.getServiceProviderConfig()

        response.statusCode shouldBe 200

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "GET"
        requestSlot.captured.url shouldBe "$baseUrl/ServiceProviderConfig"
    }

    @Test
    fun `close delegates to transport`() {
        client.close()

        verify { transport.close() }
    }

    @Test
    fun `error response with null body throws ScimClientException with null scimError`() {
        every { transport.execute(any()) } returns HttpResponse(statusCode = 500, body = null)

        val exception = shouldThrow<ScimClientException> {
            client.get("/Users", "123", User::class)
        }

        exception.statusCode shouldBe 500
        exception.scimError shouldBe null
    }

    @Test
    fun `response with empty body on GET throws ScimClientException`() {
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = null)

        val exception = shouldThrow<ScimClientException> {
            client.get("/Users", "123", User::class)
        }

        exception.statusCode shouldBe 200
        exception.message shouldNotBe null
    }

    @Test
    fun `bulk sends POST to Bulk endpoint`() {
        val bulkRequest = BulkRequest(operations = emptyList())
        val bulkResponse = BulkResponse(operations = emptyList())
        val serializedBody = """{"Operations":[]}""".toByteArray()
        val responseBody = """{"Operations":[]}""".toByteArray()

        every { serializer.serialize(bulkRequest) } returns serializedBody
        every { serializer.deserialize(responseBody, BulkResponse::class) } returns bulkResponse
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.bulk(bulkRequest)

        response.statusCode shouldBe 200
        response.value shouldBe bulkResponse

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "POST"
        requestSlot.captured.url shouldBe "$baseUrl/Bulk"
    }

    @Test
    fun `getSchemas sends GET to Schemas endpoint`() {
        val listResponse = ListResponse<com.marcosbarbero.scim2.core.domain.model.schema.Schema>(
            totalResults = 0,
            resources = emptyList(),
        )
        val responseBody = """{"totalResults":0}""".toByteArray()

        @Suppress("UNCHECKED_CAST")
        every { serializer.deserialize(responseBody, ListResponse::class as KClass<ListResponse<*>>) } returns listResponse
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.getSchemas()

        response.statusCode shouldBe 200

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "GET"
        requestSlot.captured.url shouldBe "$baseUrl/Schemas"
    }

    @Test
    fun `getResourceTypes sends GET to ResourceTypes endpoint`() {
        val listResponse = ListResponse<com.marcosbarbero.scim2.core.domain.model.schema.ResourceType>(
            totalResults = 0,
            resources = emptyList(),
        )
        val responseBody = """{"totalResults":0}""".toByteArray()

        @Suppress("UNCHECKED_CAST")
        every { serializer.deserialize(responseBody, ListResponse::class as KClass<ListResponse<*>>) } returns listResponse
        every { transport.execute(any()) } returns HttpResponse(statusCode = 200, body = responseBody)

        val response = client.getResourceTypes()

        response.statusCode shouldBe 200

        val requestSlot = slot<HttpRequest>()
        verify { transport.execute(capture(requestSlot)) }
        requestSlot.captured.method shouldBe "GET"
        requestSlot.captured.url shouldBe "$baseUrl/ResourceTypes"
    }
}
