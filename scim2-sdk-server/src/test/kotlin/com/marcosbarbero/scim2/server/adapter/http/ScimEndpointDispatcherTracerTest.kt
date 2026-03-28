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
package com.marcosbarbero.scim2.server.adapter.http

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.core.observability.ScimTracer
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import kotlin.reflect.KClass

class ScimEndpointDispatcherTracerTest {

    private val objectMapper = jacksonObjectMapper()
    private val config = ScimServerConfig()
    private val users = mutableMapOf<String, User>()

    private val serializer = object : ScimSerializer {
        override fun <T : Any> serialize(value: T): ByteArray = objectMapper.writeValueAsBytes(value)
        override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T = objectMapper.readValue(bytes, type.java)
        override fun serializeToString(value: Any): String = objectMapper.writeValueAsString(value)
        override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T = objectMapper.readValue(json, type.java)
        override fun enrichMetaLocation(json: ByteArray, location: String, resourceType: String?): ByteArray {
            val tree = objectMapper.readTree(json) as tools.jackson.databind.node.ObjectNode
            val metaNode = tree.get("meta")
            val meta = if (metaNode != null && metaNode is tools.jackson.databind.node.ObjectNode) {
                metaNode
            } else {
                objectMapper.createObjectNode().also { tree.set("meta", it) }
            }
            meta.put("location", location)
            if (resourceType != null && !meta.has("resourceType")) {
                meta.put("resourceType", resourceType)
            }
            return objectMapper.writeValueAsBytes(tree)
        }
    }

    /** Captures the MDC correlation ID during handler execution */
    private var capturedMdcCorrelationId: String? = null

    private val userHandler = object : ResourceHandler<User> {
        override val resourceType: Class<User> = User::class.java
        override val endpoint: String = "/Users"

        override fun get(id: String, context: ScimRequestContext): User {
            capturedMdcCorrelationId = MDC.get("scim.correlationId")
            return users[id] ?: throw ResourceNotFoundException("User not found: $id")
        }

        override fun create(resource: User, context: ScimRequestContext): User {
            capturedMdcCorrelationId = MDC.get("scim.correlationId")
            val id = UUID.randomUUID().toString()
            val created = resource.copy(id = id)
            users[id] = created
            return created
        }

        override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User {
            capturedMdcCorrelationId = MDC.get("scim.correlationId")
            if (!users.containsKey(id)) throw ResourceNotFoundException("User not found: $id")
            val replaced = resource.copy(id = id)
            users[id] = replaced
            return replaced
        }

        override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User {
            capturedMdcCorrelationId = MDC.get("scim.correlationId")
            val existing = users[id] ?: throw ResourceNotFoundException("User not found: $id")
            return existing
        }

        override fun delete(id: String, version: String?, context: ScimRequestContext) {
            capturedMdcCorrelationId = MDC.get("scim.correlationId")
            if (!users.containsKey(id)) throw ResourceNotFoundException("User not found: $id")
            users.remove(id)
        }

        override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> = ListResponse(totalResults = users.size, resources = users.values.toList())
    }

    private class CapturingEventPublisher : ScimEventPublisher {
        val events = mutableListOf<ScimEvent>()
        override fun publish(event: ScimEvent) {
            events.add(event)
        }
    }

    private class FixedCorrelationIdTracer(private val correlationId: String) : ScimTracer {
        override fun <T> trace(operationName: String, attributes: Map<String, String>, block: () -> T): T = block()
        override fun currentCorrelationId(): String = correlationId
    }

    private val schemaRegistry = SchemaRegistry()
    private lateinit var discoveryService: DiscoveryService

    @BeforeEach
    fun setup() {
        users.clear()
        capturedMdcCorrelationId = null
        schemaRegistry.register(User::class)
        discoveryService = DiscoveryService(listOf(userHandler), schemaRegistry, config)
    }

    private fun createDispatcher(
        eventPublisher: ScimEventPublisher,
        tracer: ScimTracer,
    ): ScimEndpointDispatcher = ScimEndpointDispatcher(
        handlers = listOf(userHandler),
        bulkHandler = null,
        meHandler = null,
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
        eventPublisher = eventPublisher,
        tracer = tracer,
    )

    private fun createTestUser(): User {
        val id = UUID.randomUUID().toString()
        val user = User(id = id, userName = "user-$id")
        users[id] = user
        return user
    }

    // --- Correlation ID propagation tests ---

    @Test
    fun `POST event carries correlation ID from tracer`() {
        val publisher = CapturingEventPublisher()
        val tracer = FixedCorrelationIdTracer("post-corr-123")
        val dispatcher = createDispatcher(publisher, tracer)

        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "testuser"),
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body,
        )

        dispatcher.dispatch(request)

        publisher.events.size shouldBe 1
        val event = publisher.events[0] as ResourceCreatedEvent
        event.correlationId shouldBe "post-corr-123"
    }

    @Test
    fun `PUT event carries correlation ID from tracer`() {
        val publisher = CapturingEventPublisher()
        val tracer = FixedCorrelationIdTracer("put-corr-456")
        val dispatcher = createDispatcher(publisher, tracer)
        val user = createTestUser()

        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "updated"),
        )
        val request = ScimHttpRequest(
            method = HttpMethod.PUT,
            path = "${config.basePath}/Users/${user.id}",
            body = body,
        )

        dispatcher.dispatch(request)

        publisher.events.size shouldBe 1
        val event = publisher.events[0] as ResourceReplacedEvent
        event.correlationId shouldBe "put-corr-456"
    }

    @Test
    fun `DELETE event carries correlation ID from tracer`() {
        val publisher = CapturingEventPublisher()
        val tracer = FixedCorrelationIdTracer("delete-corr-789")
        val dispatcher = createDispatcher(publisher, tracer)
        val user = createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.DELETE,
            path = "${config.basePath}/Users/${user.id}",
        )

        dispatcher.dispatch(request)

        publisher.events.size shouldBe 1
        val event = publisher.events[0] as ResourceDeletedEvent
        event.correlationId shouldBe "delete-corr-789"
    }

    // --- MDC verification tests ---

    @Test
    fun `MDC contains scim correlationId during request processing`() {
        // We mock MDC because the test classpath uses slf4j-simple, which has a no-op MDC adapter.
        // Real MDC behavior would require logback-classic on the test classpath.
        val mdcValues = mutableMapOf<String, String>()
        mockkStatic(MDC::class)
        every { MDC.put(any(), any()) } answers {
            mdcValues[firstArg()] = secondArg()
        }
        every { MDC.get(any()) } answers { mdcValues[firstArg()] }
        every { MDC.remove(any()) } answers {
            mdcValues.remove(firstArg<String>())
            Unit
        }

        try {
            val tracer = FixedCorrelationIdTracer("mdc-corr-id-abc")
            val publisher = CapturingEventPublisher()
            val dispatcher = createDispatcher(publisher, tracer)
            val user = createTestUser()

            val request = ScimHttpRequest(
                method = HttpMethod.GET,
                path = "${config.basePath}/Users/${user.id}",
            )

            dispatcher.dispatch(request)

            // Verify the dispatcher sets MDC with the tracer's correlation ID
            verify { MDC.put("scim.correlationId", "mdc-corr-id-abc") }
            // Also verify the handler captured the correlation ID via MDC.get() during execution
            capturedMdcCorrelationId shouldBe "mdc-corr-id-abc"
        } finally {
            unmockkStatic(MDC::class)
        }
    }

    @Test
    fun `MDC is cleaned up after request completes`() {
        // We mock MDC because the test classpath uses slf4j-simple, which has a no-op MDC adapter.
        // Real MDC behavior would require logback-classic on the test classpath.
        val mdcValues = mutableMapOf<String, String>()
        mockkStatic(MDC::class)
        every { MDC.put(any(), any()) } answers {
            mdcValues[firstArg()] = secondArg()
        }
        every { MDC.get(any()) } answers { mdcValues[firstArg()] }
        every { MDC.remove(any()) } answers {
            mdcValues.remove(firstArg<String>())
            Unit
        }

        try {
            val tracer = FixedCorrelationIdTracer("mdc-cleanup-test")
            val publisher = CapturingEventPublisher()
            val dispatcher = createDispatcher(publisher, tracer)
            val user = createTestUser()

            val request = ScimHttpRequest(
                method = HttpMethod.GET,
                path = "${config.basePath}/Users/${user.id}",
            )

            dispatcher.dispatch(request)

            // Verify MDC keys are removed after request processing (cleanup in finally block)
            verify { MDC.remove("scim.correlationId") }
            verify { MDC.remove("scim.operation") }
            verify { MDC.remove("scim.resourceType") }
        } finally {
            unmockkStatic(MDC::class)
        }
    }
}
