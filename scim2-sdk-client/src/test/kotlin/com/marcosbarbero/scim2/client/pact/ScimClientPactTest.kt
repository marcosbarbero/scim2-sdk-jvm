package com.marcosbarbero.scim2.client.pact

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTest
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import com.marcosbarbero.scim2.client.api.ScimClientBuilder
import com.marcosbarbero.scim2.client.api.createUser
import com.marcosbarbero.scim2.client.api.getUser
import com.marcosbarbero.scim2.client.api.searchUsers
import com.marcosbarbero.scim2.client.api.deleteUser
import com.marcosbarbero.scim2.client.error.ScimClientException
import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.domain.ScimUrns
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
class ScimClientPactTest {

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
                body = responseBody
            )
        }

        override fun close() { /* no-op */ }
    }

    private fun userResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.stringType("id", "123")
        body.stringType("userName", "john.doe")
        body.stringType("displayName", "John Doe")
        val schemas = body.array("schemas")
        schemas.stringValue(ScimUrns.USER)
        schemas.closeArray()
        val meta = body.`object`("meta")
        meta.stringType("resourceType", "User")
        meta.stringValue("created", "2026-01-01T00:00:00Z")
        meta.stringValue("lastModified", "2026-01-01T00:00:00Z")
        meta.closeObject()
        return body
    }

    private fun userGetResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        body.stringValue("id", "123")
        body.stringType("userName")
        val schemas = body.array("schemas")
        schemas.stringValue(ScimUrns.USER)
        schemas.closeArray()
        val meta = body.`object`("meta")
        meta.stringType("resourceType", "User")
        meta.closeObject()
        return body
    }

    private fun errorResponseBody(status: String): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue(ScimUrns.ERROR)
        schemas.closeArray()
        body.stringValue("status", status)
        return body
    }

    private fun listResponseBody(): PactDslJsonBody {
        val body = PactDslJsonBody()
        val schemas = body.array("schemas")
        schemas.stringValue(ScimUrns.LIST_RESPONSE)
        schemas.closeArray()
        body.integerType("totalResults")
        body.integerType("startIndex", 1)
        body.integerType("itemsPerPage")
        val resources = body.eachLike("Resources")
        resources.stringType("id")
        resources.stringType("userName")
        val resourceSchemas = resources.array("schemas")
        resourceSchemas.stringValue(ScimUrns.USER)
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

    @Pact(consumer = "ScimClient")
    fun createUserPact(builder: PactDslWithProvider): V4Pact = builder
        .given("no users exist")
        .uponReceiving("a request to create a user")
        .path("/scim/v2/Users")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
        .willRespondWith()
        .status(201)
        .headers(mapOf("Content-Type" to "application/scim+json", "Location" to "/scim/v2/Users/123"))
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
    fun searchUsersPact(builder: PactDslWithProvider): V4Pact = builder
        .given("users exist")
        .uponReceiving("a request to search users via POST .search")
        .path("/scim/v2/Users/.search")
        .method("POST")
        .headers("Content-Type", "application/scim+json")
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

    // === Test methods ===

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

    @Test
    @PactTestFor(pactMethod = "serviceProviderConfigPact")
    fun `get service provider config returns capabilities`(mockServer: MockServer) {
        val client = buildClient(mockServer)
        val response = client.getServiceProviderConfig()
        response.statusCode shouldBe 200
    }

    private fun buildClient(mockServer: MockServer) = ScimClientBuilder()
        .baseUrl(mockServer.getUrl() + "/scim/v2")
        .transport(JdkHttpTransport())
        .serializer(serializer)
        .build()
}
