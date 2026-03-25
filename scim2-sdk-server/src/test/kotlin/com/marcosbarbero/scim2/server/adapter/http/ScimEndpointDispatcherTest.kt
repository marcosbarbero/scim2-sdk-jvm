package com.marcosbarbero.scim2.server.adapter.http

import tools.jackson.module.kotlin.jacksonObjectMapper
import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse
import com.marcosbarbero.scim2.core.domain.model.error.ResourceConflictException
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ScimError
import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.error.ScimProblemDetail
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourcePatchedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.core.observability.ScimTracer
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.engine.ETagEngine
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import com.marcosbarbero.scim2.server.interceptor.ScimInterceptor
import com.marcosbarbero.scim2.server.port.AuthorizationEvaluator
import com.marcosbarbero.scim2.server.port.BulkHandler
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
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

        override fun get(id: String, context: ScimRequestContext): User =
            users[id] ?: throw ResourceNotFoundException("User not found: $id")

        override fun create(resource: User, context: ScimRequestContext): User {
            val id = java.util.UUID.randomUUID().toString()
            val created = resource.copy(id = id)
            users[id] = created
            return created
        }

        override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User {
            if (!users.containsKey(id)) throw ResourceNotFoundException("User not found: $id")
            val replaced = resource.copy(id = id)
            users[id] = replaced
            return replaced
        }

        override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User {
            val existing = users[id] ?: throw ResourceNotFoundException("User not found: $id")
            // Simple patch: just return existing for test purposes
            return existing
        }

        override fun delete(id: String, version: String?, context: ScimRequestContext) {
            if (!users.containsKey(id)) throw ResourceNotFoundException("User not found: $id")
            users.remove(id)
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
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to userName)
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
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to newUserName)
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

            override fun get(id: String, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun create(resource: User, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User =
                throw ResourceConflictException("User already exists")

            override fun delete(id: String, version: String?, context: ScimRequestContext) =
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
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "test")
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

    // --- Authorization tests ---

    private fun denyAllEvaluator() = object : AuthorizationEvaluator {
        override fun canCreate(resourceType: String, context: ScimRequestContext): Boolean = false
        override fun canRead(resourceType: String, resourceId: String, context: ScimRequestContext): Boolean = false
        override fun canUpdate(resourceType: String, resourceId: String, context: ScimRequestContext): Boolean = false
        override fun canDelete(resourceType: String, resourceId: String, context: ScimRequestContext): Boolean = false
        override fun canSearch(resourceType: String, context: ScimRequestContext): Boolean = false
        override fun canBulk(context: ScimRequestContext): Boolean = false
    }

    private fun dispatcherWithAuth(evaluator: AuthorizationEvaluator?) = ScimEndpointDispatcher(
        handlers = listOf(userHandler),
        bulkHandler = null,
        meHandler = null,
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
        authorizationEvaluator = evaluator
    )

    @Test
    fun `create with authorization denied returns 403`() {
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "test")
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        val response = dispatcherWithAuth(denyAllEvaluator()).dispatch(request)

        response.status shouldBe 403
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.detail shouldContain "Forbidden"
    }

    @Test
    fun `get with authorization denied returns 403`() {
        val user = createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/${user.id}"
        )

        val response = dispatcherWithAuth(denyAllEvaluator()).dispatch(request)

        response.status shouldBe 403
    }

    @Test
    fun `delete with authorization denied returns 403`() {
        val user = createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.DELETE,
            path = "${config.basePath}/Users/${user.id}"
        )

        val response = dispatcherWithAuth(denyAllEvaluator()).dispatch(request)

        response.status shouldBe 403
    }

    @Test
    fun `search with authorization denied returns 403`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users"
        )

        val response = dispatcherWithAuth(denyAllEvaluator()).dispatch(request)

        response.status shouldBe 403
    }

    @Test
    fun `null evaluator allows all operations`() {
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "test")
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        val response = dispatcherWithAuth(null).dispatch(request)

        response.status shouldBe 201
    }

    // --- Event publisher tests ---

    private class TestEventPublisher : ScimEventPublisher {
        val events = mutableListOf<ScimEvent>()
        override fun publish(event: ScimEvent) { events.add(event) }
    }

    private class TestMetrics : ScimMetrics {
        val operations = mutableListOf<String>()
        var activeRequests = 0
        override fun recordOperation(endpoint: String, method: String, status: Int, duration: Duration) {
            operations.add("$method $endpoint $status")
        }
        override fun recordFilterParse(duration: Duration, success: Boolean) {}
        override fun recordPatchOperations(endpoint: String, operationCount: Int, duration: Duration) {}
        override fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: Duration) {}
        override fun recordSearchResults(endpoint: String, totalResults: Int, duration: Duration) {}
        override fun incrementActiveRequests(endpoint: String) { activeRequests++ }
        override fun decrementActiveRequests(endpoint: String) { activeRequests-- }
    }

    private class TestTracer(private val correlationId: String? = "test-corr-id") : ScimTracer {
        val traces = mutableListOf<String>()
        override fun <T> trace(operationName: String, attributes: Map<String, String>, block: () -> T): T {
            traces.add(operationName)
            return block()
        }
        override fun currentCorrelationId(): String? = correlationId
    }

    private fun dispatcherWithObservability(
        eventPublisher: ScimEventPublisher = TestEventPublisher(),
        metrics: ScimMetrics = TestMetrics(),
        tracer: ScimTracer = TestTracer()
    ) = ScimEndpointDispatcher(
        handlers = listOf(userHandler),
        bulkHandler = null,
        meHandler = null,
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
        eventPublisher = eventPublisher,
        metrics = metrics,
        tracer = tracer
    )

    @Test
    fun `after create, eventPublisher receives ResourceCreatedEvent`() {
        val publisher = TestEventPublisher()
        val d = dispatcherWithObservability(eventPublisher = publisher)
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to faker.name.firstName())
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        d.dispatch(request)

        publisher.events shouldHaveSize 1
        publisher.events[0].shouldBeInstanceOf<ResourceCreatedEvent>()
        (publisher.events[0] as ResourceCreatedEvent).resourceType shouldBe "User"
    }

    @Test
    fun `after replace, eventPublisher receives ResourceReplacedEvent`() {
        val publisher = TestEventPublisher()
        val d = dispatcherWithObservability(eventPublisher = publisher)
        val user = createTestUser()
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to faker.name.firstName())
        )
        val request = ScimHttpRequest(
            method = HttpMethod.PUT,
            path = "${config.basePath}/Users/${user.id}",
            body = body
        )

        d.dispatch(request)

        publisher.events shouldHaveSize 1
        publisher.events[0].shouldBeInstanceOf<ResourceReplacedEvent>()
        (publisher.events[0] as ResourceReplacedEvent).resourceId shouldBe user.id
    }

    @Test
    fun `after patch, eventPublisher receives ResourcePatchedEvent with operationCount`() {
        val publisher = TestEventPublisher()
        val d = dispatcherWithObservability(eventPublisher = publisher)
        val user = createTestUser()
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree("New Name")),
                PatchOperation(op = PatchOp.REPLACE, path = "nickName", value = objectMapper.valueToTree("Nick"))
            )
        )
        val body = objectMapper.writeValueAsBytes(patchRequest)
        val request = ScimHttpRequest(
            method = HttpMethod.PATCH,
            path = "${config.basePath}/Users/${user.id}",
            body = body
        )

        d.dispatch(request)

        publisher.events shouldHaveSize 1
        val event = publisher.events[0]
        event.shouldBeInstanceOf<ResourcePatchedEvent>()
        (event as ResourcePatchedEvent).operationCount shouldBe 2
    }

    @Test
    fun `after delete, eventPublisher receives ResourceDeletedEvent`() {
        val publisher = TestEventPublisher()
        val d = dispatcherWithObservability(eventPublisher = publisher)
        val user = createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.DELETE,
            path = "${config.basePath}/Users/${user.id}"
        )

        d.dispatch(request)

        publisher.events shouldHaveSize 1
        publisher.events[0].shouldBeInstanceOf<ResourceDeletedEvent>()
        (publisher.events[0] as ResourceDeletedEvent).resourceId shouldBe user.id
    }

    @Test
    fun `metrics recorded for operations`() {
        val metrics = TestMetrics()
        val d = dispatcherWithObservability(metrics = metrics)
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to faker.name.firstName())
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        d.dispatch(request)

        metrics.operations shouldHaveSize 1
        metrics.operations[0] shouldBe "POST /Users 201"
        metrics.activeRequests shouldBe 0
    }

    @Test
    fun `metrics activeRequests balanced after error`() {
        val metrics = TestMetrics()
        val d = dispatcherWithObservability(metrics = metrics)
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent"
        )

        d.dispatch(request)

        metrics.activeRequests shouldBe 0
        metrics.operations shouldHaveSize 1
        metrics.operations[0] shouldBe "GET /Users 404"
    }

    @Test
    fun `correlation ID propagated to events`() {
        val publisher = TestEventPublisher()
        val tracer = TestTracer("my-correlation-id")
        val d = dispatcherWithObservability(eventPublisher = publisher, tracer = tracer)
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to faker.name.firstName())
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        d.dispatch(request)

        publisher.events shouldHaveSize 1
        publisher.events[0].correlationId shouldBe "my-correlation-id"
    }

    @Test
    fun `tracer trace is invoked for operations`() {
        val tracer = TestTracer()
        val d = dispatcherWithObservability(tracer = tracer)
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to faker.name.firstName())
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body
        )

        d.dispatch(request)

        tracer.traces shouldHaveSize 1
        tracer.traces[0] shouldBe "scim.post./Users"
    }

    // --- Case-insensitive routing tests ---

    @Test
    fun `GET users lowercase should work same as Users`() {
        val user = createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/users/${user.id}"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("userName").asText() shouldBe user.userName
    }

    @Test
    fun `GET USERS uppercase should work same as Users`() {
        val user = createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/USERS/${user.id}"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("userName").asText() shouldBe user.userName
    }

    @Test
    fun `GET serviceproviderconfig lowercase returns config`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/serviceproviderconfig"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("patch").get("supported").asBoolean() shouldBe true
    }

    @Test
    fun `POST bulk lowercase works`() {
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
            path = "${config.basePath}/bulk",
            body = objectMapper.writeValueAsBytes(bulkRequest)
        )

        val response = dispatcherWithBulk.dispatch(request)

        response.status shouldBe 200
    }

    @Test
    fun `POST users dot Search case insensitive should delegate to search`() {
        createTestUser()

        val searchRequest = SearchRequest(filter = "userName eq \"test\"")
        val body = objectMapper.writeValueAsBytes(searchRequest)

        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/users/.Search",
            body = body
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
    }

    // --- ProblemDetail content negotiation tests ---

    @Test
    fun `error with Accept problem+json returns ProblemDetail format`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent-id",
            headers = mapOf("Accept" to listOf("application/problem+json"))
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 404
        response.headers["Content-Type"] shouldBe "application/problem+json"
        val problemDetail = objectMapper.readValue(response.body, ScimProblemDetail::class.java)
        problemDetail.status shouldBe 404
        problemDetail.type shouldBe "about:blank"
        problemDetail.title shouldBe "Not Found"
        problemDetail.detail shouldNotBe null
    }

    @Test
    fun `error with Accept scim+json returns ScimError format`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent-id",
            headers = mapOf("Accept" to listOf("application/scim+json"))
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 404
        response.headers["Content-Type"] shouldBe "application/scim+json"
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.status shouldBe "404"
    }

    @Test
    fun `error without Accept header returns ScimError format by default`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent-id"
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 404
        response.headers["Content-Type"] shouldBe "application/scim+json"
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.status shouldBe "404"
    }

    @Test
    fun `error with Accept problem+json includes scimType when present`() {
        val conflictHandler = object : ResourceHandler<User> {
            override val resourceType: Class<User> = User::class.java
            override val endpoint: String = "/Users"
            override fun get(id: String, context: ScimRequestContext): User = throw ResourceConflictException("duplicate")
            override fun create(resource: User, context: ScimRequestContext): User = throw ResourceConflictException("duplicate")
            override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User = throw ResourceConflictException("duplicate")
            override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User = throw ResourceConflictException("duplicate")
            override fun delete(id: String, version: String?, context: ScimRequestContext) = throw ResourceConflictException("duplicate")
            override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> = throw ResourceConflictException("duplicate")
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
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "test")
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            headers = mapOf("Accept" to listOf("application/problem+json")),
            body = body
        )

        val response = conflictDispatcher.dispatch(request)

        response.status shouldBe 409
        response.headers["Content-Type"] shouldBe "application/problem+json"
        val problemDetail = objectMapper.readValue(response.body, ScimProblemDetail::class.java)
        problemDetail.scimType shouldBe "uniqueness"
        problemDetail.type shouldBe "urn:ietf:params:scim:api:messages:2.0:Error:uniqueness"
    }

    // --- Property enforcement tests ---

    private fun dispatcherWithConfig(
        customConfig: ScimServerConfig,
        customBulkHandler: BulkHandler? = null
    ) = ScimEndpointDispatcher(
        handlers = listOf(userHandler),
        bulkHandler = customBulkHandler,
        meHandler = null,
        discoveryService = DiscoveryService(listOf(userHandler), schemaRegistry, customConfig),
        config = customConfig,
        serializer = serializer
    )

    @Test
    fun `bulk disabled returns 501`() {
        val bulkHandler = object : BulkHandler {
            override fun processBulk(request: BulkRequest, context: ScimRequestContext): BulkResponse =
                BulkResponse(operations = emptyList())
        }
        val d = dispatcherWithConfig(
            config.copy(bulkEnabled = false),
            customBulkHandler = bulkHandler
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Bulk",
            body = objectMapper.writeValueAsBytes(BulkRequest(operations = emptyList()))
        )

        val response = d.dispatch(request)

        response.status shouldBe 501
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.detail shouldContain "Bulk operations are not supported"
    }

    @Test
    fun `patch disabled returns 501`() {
        val d = dispatcherWithConfig(config.copy(patchEnabled = false))
        val user = createTestUser()
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree("New"))
            )
        )
        val request = ScimHttpRequest(
            method = HttpMethod.PATCH,
            path = "${config.basePath}/Users/${user.id}",
            body = objectMapper.writeValueAsBytes(patchRequest)
        )

        val response = d.dispatch(request)

        response.status shouldBe 501
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.detail shouldContain "PATCH operations are not supported"
    }

    @Test
    fun `filter disabled rejects filter param`() {
        val d = dispatcherWithConfig(config.copy(filterEnabled = false))
        createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
            queryParameters = mapOf("filter" to listOf("userName eq \"test\""))
        )

        val response = d.dispatch(request)

        response.status shouldBe 400
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.detail shouldContain "Filtering is not supported"
    }

    @Test
    fun `sort disabled rejects sortBy param`() {
        val d = dispatcherWithConfig(config.copy(sortEnabled = false))
        createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
            queryParameters = mapOf("sortBy" to listOf("userName"))
        )

        val response = d.dispatch(request)

        response.status shouldBe 400
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.detail shouldContain "Sorting is not supported"
    }

    @Test
    fun `filter max-results caps results`() {
        repeat(5) { i -> createTestUser() }

        val d = dispatcherWithConfig(config.copy(filterMaxResults = 2))
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users"
        )

        val response = d.dispatch(request)

        response.status shouldBe 200
        val responseBody = objectMapper.readTree(response.body)
        responseBody.get("Resources").size() shouldBe 2
        responseBody.get("itemsPerPage").asInt() shouldBe 2
    }

    @Test
    fun `bulk max-payload-size rejects oversized requests`() {
        val bulkHandler = object : BulkHandler {
            override fun processBulk(request: BulkRequest, context: ScimRequestContext): BulkResponse =
                BulkResponse(operations = emptyList())
        }
        val d = dispatcherWithConfig(
            config.copy(bulkMaxPayloadSize = 10),
            customBulkHandler = bulkHandler
        )
        val largeBody = ByteArray(100) { 0 }
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Bulk",
            body = largeBody
        )

        val response = d.dispatch(request)

        response.status shouldBe 413
        val error = objectMapper.readValue(response.body, ScimError::class.java)
        error.detail shouldContain "maximum size"
    }

    @Test
    fun `pagination default-page-size used when count not specified`() {
        repeat(5) { createTestUser() }

        // Use a handler that captures the SearchRequest to verify the effective count
        var capturedCount: Int? = null
        val capturingHandler = object : ResourceHandler<User> {
            override val resourceType: Class<User> = User::class.java
            override val endpoint: String = "/Users"
            override fun get(id: String, context: ScimRequestContext): User = users[id]!!
            override fun create(resource: User, context: ScimRequestContext): User = resource
            override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User = resource
            override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User = users[id]!!
            override fun delete(id: String, version: String?, context: ScimRequestContext) {}
            override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> {
                capturedCount = request.count
                return ListResponse(totalResults = users.size, resources = users.values.toList())
            }
        }

        val customConfig = config.copy(defaultPageSize = 3)
        val d = ScimEndpointDispatcher(
            handlers = listOf(capturingHandler),
            bulkHandler = null,
            meHandler = null,
            discoveryService = DiscoveryService(listOf(capturingHandler), schemaRegistry, customConfig),
            config = customConfig,
            serializer = serializer
        )

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users"
        )

        d.dispatch(request)

        capturedCount shouldBe 3
    }

    @Test
    fun `pagination max-page-size caps count`() {
        createTestUser()

        var capturedCount: Int? = null
        val capturingHandler = object : ResourceHandler<User> {
            override val resourceType: Class<User> = User::class.java
            override val endpoint: String = "/Users"
            override fun get(id: String, context: ScimRequestContext): User = users[id]!!
            override fun create(resource: User, context: ScimRequestContext): User = resource
            override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User = resource
            override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User = users[id]!!
            override fun delete(id: String, version: String?, context: ScimRequestContext) {}
            override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> {
                capturedCount = request.count
                return ListResponse(totalResults = 0, resources = emptyList())
            }
        }

        val customConfig = config.copy(maxPageSize = 5)
        val d = ScimEndpointDispatcher(
            handlers = listOf(capturingHandler),
            bulkHandler = null,
            meHandler = null,
            discoveryService = DiscoveryService(listOf(capturingHandler), schemaRegistry, customConfig),
            config = customConfig,
            serializer = serializer
        )

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
            queryParameters = mapOf("count" to listOf("9999"))
        )

        d.dispatch(request)

        capturedCount shouldBe 5
    }

    private fun createTestUser(): User {
        val id = java.util.UUID.randomUUID().toString()
        val user = User(id = id, userName = faker.name.firstName())
        users[id] = user
        return user
    }
}
