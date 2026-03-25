package com.marcosbarbero.scim2.server.adapter.http

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse
import com.marcosbarbero.scim2.core.domain.model.error.ResourceConflictException
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ScimError
import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.engine.ETagEngine
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import com.marcosbarbero.scim2.server.interceptor.ScimInterceptor
import com.marcosbarbero.scim2.server.port.BulkHandler
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ScimEndpointDispatcherTest {

    private val faker = Faker()
    private val objectMapper = jacksonObjectMapper()
    private val config = ScimServerConfig()
    private val users = mutableMapOf<String, User>()

    private val serializer = object : ScimSerializer {
        override fun <T : Any> serialize(value: T): ByteArray = objectMapper.writeValueAsBytes(value)
        override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T = objectMapper.readValue(bytes, type.java)
        override fun serializeToString(value: Any): String = objectMapper.writeValueAsString(value)
        override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T = objectMapper.readValue(json, type.java)
    }

    private val userHandler = object : ResourceHandler<User> {
        override val resourceType: Class<User> = User::class.java
        override val endpoint: String = "/Users"

        override fun get(id: ResourceId, context: ScimRequestContext): User =
            users[id.value] ?: throw ResourceNotFoundException("User not found: ${id.value}")

        override fun create(resource: User, context: ScimRequestContext): User {
            val id = java.util.UUID.randomUUID().toString()
            val created = resource.copy(id = id)
            users[id] = created
            return created
        }

        override fun replace(id: ResourceId, resource: User, version: ETag?, context: ScimRequestContext): User {
            if (!users.containsKey(id.value)) throw ResourceNotFoundException("User not found: ${id.value}")
            val replaced = resource.copy(id = id.value)
            users[id.value] = replaced
            return replaced
        }

        override fun patch(id: ResourceId, request: PatchRequest, version: ETag?, context: ScimRequestContext): User {
            val existing = users[id.value] ?: throw ResourceNotFoundException("User not found: ${id.value}")
            // Simple patch: just return existing for test purposes
            return existing
        }

        override fun delete(id: ResourceId, version: ETag?, context: ScimRequestContext) {
            if (!users.containsKey(id.value)) throw ResourceNotFoundException("User not found: ${id.value}")
            users.remove(id.value)
        }

        override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> =
            ListResponse(totalResults = users.size, resources = users.values.toList())
    }

    private val schemaRegistry = SchemaRegistry()
    private lateinit var discoveryService: DiscoveryService
    private lateinit var dispatcher: ScimEndpointDispatcher

    @BeforeEach
    fun setup() {
        users.clear()
        schemaRegistry.register(User::class)
        discoveryService = DiscoveryService(listOf(userHandler), schemaRegistry, config)
        dispatcher = ScimEndpointDispatcher(
            handlers = listOf(userHandler),
            bulkHandler = null,
            meHandler = null,
            discoveryService = discoveryService,
            config = config,
            serializer = serializer
        )
    }

    @Test
    fun `POST Users should create and return 201`() {
        val userName = faker.name.firstName()
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf("urn:ietf:params:scim:schemas:core:2.0:User"), "userName" to userName)
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 201
        response.headers["Location"] shouldNotBe null
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("userName").asText() shouldBe userName
        responseBody.get("id").asText() shouldNotBe null
    }

    @Test
    fun `GET Users by id should return 200`() {
        val user = createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/${user.id}"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("userName").asText() shouldBe user.userName
    }

    @Test
    fun `PUT Users by id should replace and return 200`() {
        val user = createTestUser()
        val newUserName = faker.name.firstName()
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf("urn:ietf:params:scim:schemas:core:2.0:User"), "userName" to newUserName)
        )

        val request = ScimHttpRequest(
            method = HttpMethod.PUT,
            path = "${config.basePath}/Users/${user.id}",
            body = body
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("userName").asText() shouldBe newUserName
    }

    @Test
    fun `PATCH Users by id should apply patch and return 200`() {
        val user = createTestUser()
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree("New Name"))
            )
        )
        val body = objectMapper.writeValueAsBytes(patchRequest)

        val request = ScimHttpRequest(
            method = HttpMethod.PATCH,
            path = "${config.basePath}/Users/${user.id}",
            body = body
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
    }

    @Test
    fun `DELETE Users by id should return 204`() {
        val user = createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.DELETE,
            path = "${config.basePath}/Users/${user.id}"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 204
    }

    @Test
    fun `GET Users with filter should delegate to search`() {
        createTestUser()
        createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
            queryParameters = mapOf("filter" to listOf("userName eq \"test\""))
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("totalResults").asInt() shouldBe 2
    }

    @Test
    fun `POST Users dot search should delegate to search`() {
        createTestUser()

        val searchRequest = SearchRequest(filter = "userName eq \"test\"")
        val body = objectMapper.writeValueAsBytes(searchRequest)

        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users/.search",
            body = body
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
    }

    @Test
    fun `GET ServiceProviderConfig should return config`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/ServiceProviderConfig"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("patch").get("supported").asBoolean() shouldBe true
    }

    @Test
    fun `GET Schemas should return schemas`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Schemas"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("totalResults").asInt() shouldBe 1
    }

    @Test
    fun `GET ResourceTypes should return resource types`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/ResourceTypes"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("totalResults").asInt() shouldBe 1
    }

    @Test
    fun `unknown endpoint should return 404`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Unknown/123"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 404
    }

    @Test
    fun `handler throwing ResourceNotFoundException should return 404 with ScimError body`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent-id"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 404
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.status shouldBe "404"
        error.detail shouldNotBe null
    }

    @Test
    fun `handler throwing ResourceConflictException should return 409`() {
        // Create a handler that throws conflict
        val conflictHandler = object : ResourceHandler<User> {
            override val resourceType: Class<User> = User::class.java
            override val endpoint: String = "/Users"

            override fun get(id: ResourceId, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun create(resource: User, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun replace(id: ResourceId, resource: User, version: ETag?, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun patch(id: ResourceId, request: PatchRequest, version: ETag?, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun delete(id: ResourceId, version: ETag?, context: ScimRequestContext) =
                throw ResourceConflictException("User already exists")

            override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> =
                throw ResourceConflictException("User already exists")
        }

        val conflictDispatcher = ScimEndpointDispatcher(
            handlers = listOf(conflictHandler),
            bulkHandler = null,
            meHandler = null,
            discoveryService = discoveryService,
            config = config,
            serializer = serializer
        )

        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf("urn:ietf:params:scim:schemas:core:2.0:User"), "userName" to "test")
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        val response = conflictDispatcher.dispatch(request)

        response.status shouldBe 409
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.status shouldBe "409"
        error.scimType shouldBe "uniqueness"
    }

    @Test
    fun `interceptors should be applied in order`() {
        val callOrder = mutableListOf<String>()

        val first = object : ScimInterceptor {
            override val order: Int = 1
            override fun preHandle(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpRequest {
                callOrder.add("pre-1")
                return request
            }
            override fun postHandle(request: ScimHttpRequest, response: ScimHttpResponse, context: ScimRequestContext): ScimHttpResponse {
                callOrder.add("post-1")
                return response
            }
        }

        val second = object : ScimInterceptor {
            override val order: Int = 2
            override fun preHandle(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpRequest {
                callOrder.add("pre-2")
                return request
            }
            override fun postHandle(request: ScimHttpRequest, response: ScimHttpResponse, context: ScimRequestContext): ScimHttpResponse {
                callOrder.add("post-2")
                return response
            }
        }

        val dispatcherWithInterceptors = ScimEndpointDispatcher(
            handlers = listOf(userHandler),
            bulkHandler = null,
            meHandler = null,
            discoveryService = discoveryService,
            config = config,
            serializer = serializer,
            interceptors = listOf(second, first)  // deliberately out of order
        )

        createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
        )

        dispatcherWithInterceptors.dispatch(request)

        callOrder shouldBe listOf("pre-1", "pre-2", "post-1", "post-2")
    }

    @Test
    fun `GET Users list without filter should return search results`() {
        createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
    }

    @Test
    fun `POST Bulk should delegate to bulk handler`() {
        val bulkHandler = object : BulkHandler {
            override fun processBulk(request: BulkRequest, context: ScimRequestContext): BulkResponse =
                BulkResponse(operations = emptyList())
        }

        val dispatcherWithBulk = ScimEndpointDispatcher(
            handlers = listOf(userHandler),
            bulkHandler = bulkHandler,
            meHandler = null,
            discoveryService = discoveryService,
            config = config,
            serializer = serializer
        )

        val bulkRequest = BulkRequest(operations = emptyList())
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Bulk",
            body = objectMapper.writeValueAsBytes(bulkRequest)
        )

        val response = dispatcherWithBulk.dispatch(request)

        response.status shouldBe 200
    }

    @Test
    fun `POST Bulk without handler should return 501`() {
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Bulk",
            body = objectMapper.writeValueAsBytes(BulkRequest(operations = emptyList()))
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 501
    }

    private fun createTestUser(): User {
        val id = java.util.UUID.randomUUID().toString()
        val user = User(id = id, userName = faker.name.firstName())
        users[id] = user
        return user
    }
}
