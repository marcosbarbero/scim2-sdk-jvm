package com.marcosbarbero.scim2.test.contract

import tools.jackson.databind.JsonNode
import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Abstract contract test that validates HTTP-level RFC 7644 compliance.
 * Tests verify response status codes, headers, and body format at the HTTP layer.
 *
 * Extend this class and implement [createDispatcher] to test any SCIM endpoint implementation.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7644">RFC 7644: SCIM Protocol</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7643">RFC 7643: SCIM Core Schema</a>
 */
abstract class ScimApiContractTest {

    abstract fun createDispatcher(): ScimEndpointDispatcher

    abstract fun sampleUserJson(): ByteArray

    private lateinit var dispatcher: ScimEndpointDispatcher
    private val objectMapper = JacksonScimSerializer.defaultObjectMapper()

    @BeforeEach
    fun setUp() {
        dispatcher = createDispatcher()
    }

    // === CREATE (POST) — RFC 7644 §3.1 ===

    /**
     * RFC 7644 §3.1: "If the service provider determines that the creation of the requested
     * resource is feasible and the request is well formed, the service provider SHALL create
     * the resource and respond with HTTP status code 201 (Created)."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.1">RFC 7644 §3.1</a>
     */
    @Test
    fun `POST returns 201 Created (RFC 7644 §3-1)`() {
        val response = post("/Users", sampleUserJson())
        response.status shouldBe 201
    }

    /**
     * RFC 7644 §3.1: "The response MUST include a Location header indicating the URI of the
     * newly created resource."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.1">RFC 7644 §3.1</a>
     */
    @Test
    fun `POST response includes Location header (RFC 7644 §3-1)`() {
        val response = post("/Users", sampleUserJson())
        response.headers["Location"].shouldNotBeNull()
        response.headers["Location"]!! shouldContain "/Users/"
    }

    /**
     * RFC 7643 §3: "The 'schemas' attribute is a REQUIRED attribute and is of type List of URIs."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7643#section-3">RFC 7643 §3</a>
     */
    @Test
    fun `POST response body contains schemas array (RFC 7643 §3)`() {
        val response = post("/Users", sampleUserJson())
        val body = parseJson(response)
        body.has("schemas") shouldBe true
        body["schemas"].isArray shouldBe true
        body["schemas"].size() shouldBeGreaterThan 0
    }

    /**
     * RFC 7644 §3.1: "The response body SHOULD contain the created resource."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.1">RFC 7644 §3.1</a>
     */
    @Test
    fun `POST response body contains id and meta (RFC 7644 §3-1)`() {
        val response = post("/Users", sampleUserJson())
        val body = parseJson(response)
        body.has("id") shouldBe true
        body["id"].asText().isNotBlank() shouldBe true
        body.has("meta") shouldBe true
        body["meta"].has("resourceType") shouldBe true
        body["meta"].has("created") shouldBe true
    }

    // === READ (GET) — RFC 7644 §3.2 ===

    /**
     * RFC 7644 §3.2: "If the resource exists, the server responds with HTTP status code 200."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.2">RFC 7644 §3.2</a>
     */
    @Test
    fun `GET by id returns 200 OK (RFC 7644 §3-2)`() {
        val created = post("/Users", sampleUserJson())
        val id = parseJson(created)["id"].asText()
        val response = get("/Users/$id")
        response.status shouldBe 200
    }

    /**
     * RFC 7644 §3.2: "If the resource does not exist, the server responds with HTTP status code 404."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.2">RFC 7644 §3.2</a>
     */
    @Test
    fun `GET non-existent returns 404 Not Found (RFC 7644 §3-2)`() {
        val response = get("/Users/nonexistent-id")
        response.status shouldBe 404
    }

    /**
     * RFC 7644 §3.12: "404 error responses ... include a detail error message ..."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.12">RFC 7644 §3.12</a>
     */
    @Test
    fun `GET 404 response is a valid ScimError (RFC 7644 §3-12)`() {
        val response = get("/Users/nonexistent-id")
        val body = parseJson(response)
        body["schemas"][0].asText() shouldBe ScimUrns.ERROR
        body["status"].asText() shouldBe "404"
    }

    // === ETAG — RFC 7644 §3.14 ===

    /**
     * RFC 7644 §3.14: "The service provider SHOULD return an ETag for the resource."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.14">RFC 7644 §3.14</a>
     */
    @Test
    fun `GET response includes ETag header (RFC 7644 §3-14)`() {
        val created = post("/Users", sampleUserJson())
        val id = parseJson(created)["id"].asText()
        val response = get("/Users/$id")
        response.headers["ETag"].shouldNotBeNull()
    }

    /**
     * RFC 7644 §3.14: "If the request includes an If-None-Match header and the ETag matches,
     * the server SHOULD respond with 304 Not Modified."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.14">RFC 7644 §3.14</a>
     */
    @Test
    fun `GET with matching If-None-Match returns 304 Not Modified (RFC 7644 §3-14)`() {
        val created = post("/Users", sampleUserJson())
        val id = parseJson(created)["id"].asText()
        val getResponse = get("/Users/$id")
        val etag = getResponse.headers["ETag"]!!
        val conditionalResponse = get("/Users/$id", headers = mapOf("If-None-Match" to listOf(etag)))
        conditionalResponse.status shouldBe 304
    }

    // === DELETE — RFC 7644 §3.6 ===

    /**
     * RFC 7644 §3.6: "the server SHALL respond with 204 (No Content)"
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.6">RFC 7644 §3.6</a>
     */
    @Test
    fun `DELETE returns 204 No Content (RFC 7644 §3-6)`() {
        val created = post("/Users", sampleUserJson())
        val id = parseJson(created)["id"].asText()
        val response = delete("/Users/$id")
        response.status shouldBe 204
    }

    // === REPLACE (PUT) — RFC 7644 §3.3 ===

    /**
     * RFC 7644 §3.3: "If the resource exists, the service provider SHALL replace..."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.3">RFC 7644 §3.3</a>
     */
    @Test
    fun `PUT returns 200 OK (RFC 7644 §3-3)`() {
        val created = post("/Users", sampleUserJson())
        val id = parseJson(created)["id"].asText()
        val response = put("/Users/$id", sampleUserJson())
        response.status shouldBe 200
    }

    // === PATCH — RFC 7644 §3.5.2 ===

    /**
     * RFC 7644 §3.5.2: "The server SHALL return ... 200 OK with the modified resource"
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.5.2">RFC 7644 §3.5.2</a>
     */
    @Test
    fun `PATCH returns 200 OK (RFC 7644 §3-5-2)`() {
        val created = post("/Users", sampleUserJson())
        val id = parseJson(created)["id"].asText()
        val patchBody = objectMapper.writeValueAsBytes(
            mapOf(
                "schemas" to listOf(ScimUrns.PATCH_OP),
                "Operations" to listOf(
                    mapOf("op" to "replace", "path" to "displayName", "value" to "Updated")
                )
            )
        )
        val response = patch("/Users/$id", patchBody)
        response.status shouldBe 200
    }

    // === SEARCH (GET with query) — RFC 7644 §3.4.2 ===

    /**
     * RFC 7644 §3.4.2: "The response body ... is a JSON structure containing the 'schemas',
     * 'totalResults', 'Resources', 'startIndex', and 'itemsPerPage' attributes."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.4.2">RFC 7644 §3.4.2</a>
     */
    @Test
    fun `GET search returns ListResponse with required fields (RFC 7644 §3-4-2)`() {
        post("/Users", sampleUserJson())
        val response = get("/Users")
        response.status shouldBe 200
        val body = parseJson(response)
        body.has("schemas") shouldBe true
        body["schemas"][0].asText() shouldBe ScimUrns.LIST_RESPONSE
        body.has("totalResults") shouldBe true
        body.has("startIndex") shouldBe true
        body.has("itemsPerPage") shouldBe true
        body.has("Resources") shouldBe true
    }

    // === SEARCH (POST /.search) — RFC 7644 §3.4.3 ===

    /**
     * RFC 7644 §3.4.3: "Clients MAY execute queries ... using HTTP POST"
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.4.3">RFC 7644 §3.4.3</a>
     */
    @Test
    fun `POST search returns ListResponse (RFC 7644 §3-4-3)`() {
        post("/Users", sampleUserJson())
        val searchBody = objectMapper.writeValueAsBytes(
            mapOf(
                "schemas" to listOf(ScimUrns.SEARCH_REQUEST),
                "startIndex" to 1,
                "count" to 10
            )
        )
        val response = postSearch("/Users/.search", searchBody)
        response.status shouldBe 200
        val body = parseJson(response)
        body["schemas"][0].asText() shouldBe ScimUrns.LIST_RESPONSE
    }

    // === DISCOVERY — RFC 7644 §4 ===

    /**
     * RFC 7644 §4: "Service provider configurations ... SHALL be retrievable."
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-4">RFC 7644 §4</a>
     */
    @Test
    fun `GET ServiceProviderConfig returns 200 (RFC 7644 §4)`() {
        val response = get("/ServiceProviderConfig")
        response.status shouldBe 200
    }

    @Test
    fun `GET Schemas returns 200 with schema list (RFC 7644 §4)`() {
        val response = get("/Schemas")
        response.status shouldBe 200
    }

    @Test
    fun `GET ResourceTypes returns 200 with resource types (RFC 7644 §4)`() {
        val response = get("/ResourceTypes")
        response.status shouldBe 200
    }

    // === CONTENT-TYPE — RFC 7644 §3.1 ===

    /**
     * RFC 7644 §3.1: "Responses ... SHOULD specify a Content-Type of 'application/scim+json'"
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.1">RFC 7644 §3.1</a>
     */
    @Test
    fun `error response Content-Type is application scim+json (RFC 7644 §3-1)`() {
        val response = get("/Users/nonexistent")
        response.headers["Content-Type"] shouldBe "application/scim+json"
    }

    /**
     * RFC 7644 §3.1: Successful create responses should include Content-Type application/scim+json.
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.1">RFC 7644 §3.1</a>
     */
    @Test
    fun `POST response Content-Type is application scim+json (RFC 7644 §3-1)`() {
        val response = post("/Users", sampleUserJson())
        response.headers["Content-Type"] shouldBe "application/scim+json"
    }

    // === Helper methods ===

    private fun post(path: String, body: ByteArray): ScimHttpResponse =
        dispatcher.dispatch(
            ScimHttpRequest(
                method = HttpMethod.POST,
                path = "${basePath()}$path",
                body = body,
                headers = mapOf("Content-Type" to listOf("application/scim+json"))
            )
        )

    private fun get(path: String, headers: Map<String, List<String>> = emptyMap()): ScimHttpResponse =
        dispatcher.dispatch(
            ScimHttpRequest(
                method = HttpMethod.GET,
                path = "${basePath()}$path",
                headers = headers
            )
        )

    private fun put(path: String, body: ByteArray): ScimHttpResponse =
        dispatcher.dispatch(
            ScimHttpRequest(
                method = HttpMethod.PUT,
                path = "${basePath()}$path",
                body = body,
                headers = mapOf("Content-Type" to listOf("application/scim+json"))
            )
        )

    private fun patch(path: String, body: ByteArray): ScimHttpResponse =
        dispatcher.dispatch(
            ScimHttpRequest(
                method = HttpMethod.PATCH,
                path = "${basePath()}$path",
                body = body,
                headers = mapOf("Content-Type" to listOf("application/scim+json"))
            )
        )

    private fun delete(path: String): ScimHttpResponse =
        dispatcher.dispatch(
            ScimHttpRequest(
                method = HttpMethod.DELETE,
                path = "${basePath()}$path"
            )
        )

    private fun postSearch(path: String, body: ByteArray): ScimHttpResponse =
        dispatcher.dispatch(
            ScimHttpRequest(
                method = HttpMethod.POST,
                path = "${basePath()}$path",
                body = body,
                headers = mapOf("Content-Type" to listOf("application/scim+json"))
            )
        )

    private fun basePath(): String = "/scim/v2"

    private fun parseJson(response: ScimHttpResponse): JsonNode =
        objectMapper.readTree(response.body)
}
