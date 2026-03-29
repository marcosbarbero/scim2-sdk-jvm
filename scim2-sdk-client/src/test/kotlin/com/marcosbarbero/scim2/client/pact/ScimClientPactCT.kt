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
package com.marcosbarbero.scim2.client.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTest
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import com.marcosbarbero.scim2.client.api.ScimClientBuilder
import com.marcosbarbero.scim2.client.api.createGroup
import com.marcosbarbero.scim2.client.api.createUser
import com.marcosbarbero.scim2.client.api.deleteGroup
import com.marcosbarbero.scim2.client.api.deleteUser
import com.marcosbarbero.scim2.client.api.getGroup
import com.marcosbarbero.scim2.client.api.getUser
import com.marcosbarbero.scim2.client.api.patchGroup
import com.marcosbarbero.scim2.client.api.patchUser
import com.marcosbarbero.scim2.client.api.replaceGroup
import com.marcosbarbero.scim2.client.api.replaceUser
import com.marcosbarbero.scim2.client.api.searchGroups
import com.marcosbarbero.scim2.client.api.searchUsers
import com.marcosbarbero.scim2.client.error.ScimClientException
import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkOperation
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.http.HttpClient

/**
 * Pact Consumer tests for the SCIM Client.
 *
 * These tests define the contract that the SCIM Client (consumer) expects
 * from any SCIM Service Provider (provider). The generated pact files
 * can be verified against any server implementation.
 */
@PactConsumerTest
@PactTestFor(providerName = "ScimServiceProvider")
class ScimClientPactCT {

    private val serializer = JacksonScimSerializer()

    /**
     * Simple JDK HttpClient-based transport for testing purposes.
     * Avoids a cyclic dependency on scim2-sdk-client-httpclient.
     */
    private class JdkHttpTransport : HttpTransport {
        private val httpClient = HttpClient.newHttpClient()

        override fun execute(request: HttpRequest): HttpResponse {
            val bodyPublisher = if (request.body != null) {
                java.net.http.HttpRequest.BodyPublishers.ofByteArray(request.body)
            } else {
                java.net.http.HttpRequest.BodyPublishers.noBody()
            }

            val builder = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(request.url))
                .method(request.method, bodyPublisher)

            request.headers.forEach { (key, value) ->
                builder.header(key, value)
            }

            val javaResponse = httpClient.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray())
            val responseHeaders = javaResponse.headers().map().mapValues { (_, values) -> values.toList() }
            val responseBody = javaResponse.body()?.takeIf { it.isNotEmpty() }

            return HttpResponse(
                statusCode = javaResponse.statusCode(),
                headers = responseHeaders,
                body = responseBody,
            )
        }

        override fun close() { /* no-op */ }
    }

    // ========================================================================
    // Response body builders
    // ========================================================================

    private fun userResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.stringType("id", "123")
        body.stringType("userName", "john.doe")
        body.stringType("displayName", "John Doe")
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:User")
        schemas.closeArray()
        val meta = body.`object`("meta")
        meta.stringType("resourceType", "User")
        meta.stringType("created", "2026-01-01T00:00:00Z")
        meta.stringType("lastModified", "2026-01-01T00:00:00Z")
        meta.closeObject()
        return body
    }

    private fun userGetResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.stringValue("id", "123")
        body.stringType("userName")
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:User")
        schemas.closeArray()
        val meta = body.`object`("meta")
        meta.stringType("resourceType", "User")
        meta.closeObject()
        return body
    }

    private fun groupResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.stringType("id", "g-456")
        body.stringType("displayName", "Engineering")
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:Group")
        schemas.closeArray()
        val meta = body.`object`("meta")
        meta.stringType("resourceType", "Group")
        meta.stringType("created", "2026-01-01T00:00:00Z")
        meta.stringType("lastModified", "2026-01-01T00:00:00Z")
        meta.closeObject()
        return body
    }

    private fun groupGetResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.stringValue("id", "g-456")
        body.stringType("displayName")
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:Group")
        schemas.closeArray()
        val meta = body.`object`("meta")
        meta.stringType("resourceType", "Group")
        meta.closeObject()
        return body
    }

    private fun errorResponseBody(status: String): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:api:messages:2.0:Error")
        schemas.closeArray()
        body.stringValue("status", status)
        return body
    }

    private fun listResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:api:messages:2.0:ListResponse")
        schemas.closeArray()
        body.integerType("totalResults")
        body.integerType("startIndex", 1)
        body.integerType("itemsPerPage")
        val resources = body.eachLike("Resources")
        resources.stringType("id")
        resources.stringType("userName")
        val resourceSchemas = resources.array("schemas")
        resourceSchemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:User")
        resourceSchemas.closeArray()
        resources.closeArray()
        return body
    }

    private fun groupListResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:api:messages:2.0:ListResponse")
        schemas.closeArray()
        body.integerType("totalResults")
        body.integerType("startIndex", 1)
        body.integerType("itemsPerPage")
        val resources = body.eachLike("Resources")
        resources.stringType("id")
        resources.stringType("displayName")
        val resourceSchemas = resources.array("schemas")
        resourceSchemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:Group")
        resourceSchemas.closeArray()
        resources.closeArray()
        return body
    }

    private fun schemasListResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:api:messages:2.0:ListResponse")
        schemas.closeArray()
        body.integerType("totalResults")
        val resources = body.eachLike("Resources")
        resources.stringType("id")
        resources.stringType("name")
        val resourceSchemas = resources.array("schemas")
        resourceSchemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:Schema")
        resourceSchemas.closeArray()
        resources.closeArray()
        return body
    }

    private fun resourceTypesListResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:api:messages:2.0:ListResponse")
        schemas.closeArray()
        body.integerType("totalResults")
        val resources = body.eachLike("Resources")
        resources.stringType("id")
        resources.stringType("name")
        resources.stringType("endpoint")
        val resourceSchemas = resources.array("schemas")
        resourceSchemas.stringValue("urn:ietf:params:scim:schemas:core:2.0:ResourceType")
        resourceSchemas.closeArray()
        resources.closeArray()
        return body
    }

    private fun serviceProviderConfigBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val patch = body.`object`("patch")
        patch.booleanType("supported")
        patch.closeObject()
        val bulk = body.`object`("bulk")
        bulk.booleanType("supported")
        bulk.closeObject()
        val filter = body.`object`("filter")
        filter.booleanType("supported")
        filter.closeObject()
        val sort = body.`object`("sort")
        sort.booleanType("supported")
        sort.closeObject()
        val etag = body.`object`("etag")
        etag.booleanType("supported")
        etag.closeObject()
        return body
    }

    // ========================================================================
    // Request body builders (type-matching for consumer, replayed by provider)
    // ========================================================================

    private fun userRequestBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.array("schemas").stringValue("urn:ietf:params:scim:schemas:core:2.0:User").closeArray()
        body.stringType("userName", "john.doe")
        body.stringType("displayName", "John Doe")
        body.array("emails").closeArray()
        body.array("phoneNumbers").closeArray()
        body.array("ims").closeArray()
        body.array("photos").closeArray()
        body.array("addresses").closeArray()
        body.array("groups").closeArray()
        body.array("entitlements").closeArray()
        body.array("roles").closeArray()
        body.array("x509Certificates").closeArray()
        return body
    }

    private fun groupRequestBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.array("schemas").stringValue("urn:ietf:params:scim:schemas:core:2.0:Group").closeArray()
        body.stringType("displayName", "Engineering")
        body.array("members").closeArray()
        return body
    }

    private fun patchRequestBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.array("schemas").stringValue("urn:ietf:params:scim:api:messages:2.0:PatchOp").closeArray()
        val ops = body.eachLike("Operations")
        ops.stringValue("op", "replace")
        ops.stringType("path", "displayName")
        ops.closeArray()
        return body
    }

    private fun searchRequestBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.array("schemas").stringValue("urn:ietf:params:scim:api:messages:2.0:SearchRequest").closeArray()
        body.integerType("startIndex", 1)
        body.integerType("count", 10)
        return body
    }

    private fun bulkRequestBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.array("schemas").stringValue("urn:ietf:params:scim:api:messages:2.0:BulkRequest").closeArray()
        val ops = body.eachLike("Operations")
        ops.stringValue("method", "POST")
        ops.stringValue("path", "/Users")
        ops.stringType("bulkId", "user-1")
        ops.closeArray()
        return body
    }

    private fun bulkResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue("urn:ietf:params:scim:api:messages:2.0:BulkResponse")
        schemas.closeArray()
        val operations = body.eachLike("Operations")
        operations.stringType("method", "POST")
        operations.stringType("status", "201")
        operations.closeArray()
        return body
    }

    // ========================================================================
    // User Pact definitions
    // ========================================================================

    @Pact(consumer = "ScimClient")
    fun createUserPact(builder: PactDslWithProvider): V4Pact = builder
        .given("no users exist")
        .uponReceiving("a request to create a user")
        .path("/scim/v2/Users")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
        .body(userRequestBody())
        .willRespondWith()
        .status(201)
        .matchHeader("Content-Type", "application/scim\\+json.*", "application/scim+json")
        .matchHeader("Location", "/scim/v2/Users/.+", "/scim/v2/Users/123")
        .body(userResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun getUserPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a user with id 123 exists")
        .uponReceiving("a request to get a user by id")
        .path("/scim/v2/Users/123")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(userGetResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun getUserNotFoundPact(builder: PactDslWithProvider): V4Pact = builder
        .given("no user with id 999 exists")
        .uponReceiving("a request to get a non-existent user")
        .path("/scim/v2/Users/999")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(errorResponseBody("404"))
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun replaceUserPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a user with id 123 exists")
        .uponReceiving("a request to replace a user")
        .path("/scim/v2/Users/123")
        .method("PUT")
        .headers("Content-Type", "application/scim+json")
        .body(userRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(userResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun patchUserPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a user with id 123 exists")
        .uponReceiving("a request to patch a user")
        .path("/scim/v2/Users/123")
        .method("PATCH")
        .headers("Content-Type", "application/scim+json")
        .body(patchRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(userGetResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun searchUsersPact(builder: PactDslWithProvider): V4Pact = builder
        .given("users exist")
        .uponReceiving("a request to search users via POST .search")
        .path("/scim/v2/Users/.search")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
        .body(searchRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(listResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun deleteUserPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a user with id 123 exists")
        .uponReceiving("a request to delete a user")
        .path("/scim/v2/Users/123")
        .method("DELETE")
        .willRespondWith()
        .status(204)
        .toPact(V4Pact::class.java)

    // ========================================================================
    // Group Pact definitions
    // ========================================================================

    @Pact(consumer = "ScimClient")
    fun createGroupPact(builder: PactDslWithProvider): V4Pact = builder
        .given("no groups exist")
        .uponReceiving("a request to create a group")
        .path("/scim/v2/Groups")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
        .body(groupRequestBody())
        .willRespondWith()
        .status(201)
        .matchHeader("Content-Type", "application/scim\\+json.*", "application/scim+json")
        .matchHeader("Location", "/scim/v2/Groups/.+", "/scim/v2/Groups/g-456")
        .body(groupResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun getGroupPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a group with id g-456 exists")
        .uponReceiving("a request to get a group by id")
        .path("/scim/v2/Groups/g-456")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(groupGetResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun getGroupNotFoundPact(builder: PactDslWithProvider): V4Pact = builder
        .given("no group with id g-999 exists")
        .uponReceiving("a request to get a non-existent group")
        .path("/scim/v2/Groups/g-999")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(errorResponseBody("404"))
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun replaceGroupPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a group with id g-456 exists")
        .uponReceiving("a request to replace a group")
        .path("/scim/v2/Groups/g-456")
        .method("PUT")
        .headers("Content-Type", "application/scim+json")
        .body(groupRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(groupResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun patchGroupPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a group with id g-456 exists")
        .uponReceiving("a request to patch a group")
        .path("/scim/v2/Groups/g-456")
        .method("PATCH")
        .headers("Content-Type", "application/scim+json")
        .body(patchRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(groupResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun deleteGroupPact(builder: PactDslWithProvider): V4Pact = builder
        .given("a group with id g-456 exists")
        .uponReceiving("a request to delete a group")
        .path("/scim/v2/Groups/g-456")
        .method("DELETE")
        .willRespondWith()
        .status(204)
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun searchGroupsPact(builder: PactDslWithProvider): V4Pact = builder
        .given("groups exist")
        .uponReceiving("a request to search groups via POST .search")
        .path("/scim/v2/Groups/.search")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
        .body(searchRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(groupListResponseBody())
        .toPact(V4Pact::class.java)

    // ========================================================================
    // Discovery Pact definitions
    // ========================================================================

    @Pact(consumer = "ScimClient")
    fun serviceProviderConfigPact(builder: PactDslWithProvider): V4Pact = builder
        .given("server is running")
        .uponReceiving("a request for service provider config")
        .path("/scim/v2/ServiceProviderConfig")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(serviceProviderConfigBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun getSchemasPact(builder: PactDslWithProvider): V4Pact = builder
        .given("server is running")
        .uponReceiving("a request to list all schemas")
        .path("/scim/v2/Schemas")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(schemasListResponseBody())
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ScimClient")
    fun getResourceTypesPact(builder: PactDslWithProvider): V4Pact = builder
        .given("server is running")
        .uponReceiving("a request to list all resource types")
        .path("/scim/v2/ResourceTypes")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(resourceTypesListResponseBody())
        .toPact(V4Pact::class.java)

    // ========================================================================
    // Bulk Pact definitions
    // ========================================================================

    @Pact(consumer = "ScimClient")
    fun bulkPact(builder: PactDslWithProvider): V4Pact = builder
        .given("server is running")
        .uponReceiving("a bulk operations request")
        .path("/scim/v2/Bulk")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
        .body(bulkRequestBody())
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/scim+json"))
        .body(bulkResponseBody())
        .toPact(V4Pact::class.java)

    // ========================================================================
    // User test methods
    // ========================================================================

    @Test
    @PactTestFor(pactMethod = "createUserPact")
    fun `create user returns 201 with id and meta`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val user = User(userName = "john.doe", displayName = "John Doe")
        val response = client.createUser(user)
        response.statusCode shouldBe 201
        response.value.id shouldNotBe null
        response.value.userName shouldBe "john.doe"
        response.location shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "getUserPact")
    fun `get user returns 200 with user data`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.getUser("123")
        response.statusCode shouldBe 200
        response.value.id shouldBe "123"
    }

    @Test
    @PactTestFor(pactMethod = "getUserNotFoundPact")
    fun `get non-existent user throws client exception with 404`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val ex = assertThrows<ScimClientException> {
            client.getUser("999")
        }
        ex.statusCode shouldBe 404
    }

    @Test
    @PactTestFor(pactMethod = "replaceUserPact")
    fun `replace user returns 200 with updated data`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val user = User(userName = "john.doe", displayName = "John Doe")
        val response = client.replaceUser("123", user)
        response.statusCode shouldBe 200
        response.value.id shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "patchUserPact")
    fun `patch user returns 200 with patched data`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = null),
            ),
        )
        val response = client.patchUser("123", patchRequest)
        response.statusCode shouldBe 200
        response.value.id shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "searchUsersPact")
    fun `search users returns list response`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.searchUsers(SearchRequest(startIndex = 1, count = 10))
        response.statusCode shouldBe 200
        response.value.totalResults shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "deleteUserPact")
    fun `delete user returns 204`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        client.deleteUser("123")
        // No exception = success (204)
    }

    // ========================================================================
    // Group test methods
    // ========================================================================

    @Test
    @PactTestFor(pactMethod = "createGroupPact")
    fun `create group returns 201 with id and meta`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val group = Group(displayName = "Engineering")
        val response = client.createGroup(group)
        response.statusCode shouldBe 201
        response.value.id shouldNotBe null
        response.value.displayName shouldBe "Engineering"
        response.location shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "getGroupPact")
    fun `get group returns 200 with group data`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.getGroup("g-456")
        response.statusCode shouldBe 200
        response.value.id shouldBe "g-456"
    }

    @Test
    @PactTestFor(pactMethod = "getGroupNotFoundPact")
    fun `get non-existent group throws client exception with 404`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val ex = assertThrows<ScimClientException> {
            client.getGroup("g-999")
        }
        ex.statusCode shouldBe 404
    }

    @Test
    @PactTestFor(pactMethod = "replaceGroupPact")
    fun `replace group returns 200 with updated data`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val group = Group(displayName = "Engineering")
        val response = client.replaceGroup("g-456", group)
        response.statusCode shouldBe 200
        response.value.id shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "patchGroupPact")
    fun `patch group returns 200 with patched data`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = null),
            ),
        )
        val response = client.patchGroup("g-456", patchRequest)
        response.statusCode shouldBe 200
        response.value.id shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "deleteGroupPact")
    fun `delete group returns 204`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        client.deleteGroup("g-456")
        // No exception = success (204)
    }

    @Test
    @PactTestFor(pactMethod = "searchGroupsPact")
    fun `search groups returns list response`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.searchGroups(SearchRequest(startIndex = 1, count = 10))
        response.statusCode shouldBe 200
        response.value.totalResults shouldNotBe null
    }

    // ========================================================================
    // Discovery test methods
    // ========================================================================

    @Test
    @PactTestFor(pactMethod = "serviceProviderConfigPact")
    fun `get service provider config returns capabilities`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.getServiceProviderConfig()
        response.statusCode shouldBe 200
    }

    @Test
    @PactTestFor(pactMethod = "getSchemasPact")
    fun `get schemas returns list of schemas`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.getSchemas()
        response.statusCode shouldBe 200
        response.value.totalResults shouldNotBe null
    }

    @Test
    @PactTestFor(pactMethod = "getResourceTypesPact")
    fun `get resource types returns list of resource types`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.getResourceTypes()
        response.statusCode shouldBe 200
        response.value.totalResults shouldNotBe null
    }

    // ========================================================================
    // Bulk test methods
    // ========================================================================

    @Test
    @PactTestFor(pactMethod = "bulkPact")
    fun `bulk request returns 200 with operation results`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val bulkRequest = BulkRequest(
            operations = listOf(
                BulkOperation(method = "POST", path = "/Users", bulkId = "user-1"),
            ),
        )
        val response = client.bulk(bulkRequest)
        response.statusCode shouldBe 200
    }

    // ========================================================================
    // Helper
    // ========================================================================

    private fun buildClient(mockServer: MockServer) = ScimClientBuilder()
        .baseUrl(mockServer.getUrl() + "/scim/v2")
        .transport(JdkHttpTransport())
        .serializer(serializer)
        .build()
}
