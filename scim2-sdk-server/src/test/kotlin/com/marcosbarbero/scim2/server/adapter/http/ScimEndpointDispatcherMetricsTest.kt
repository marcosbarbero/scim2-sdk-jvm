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
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

class ScimEndpointDispatcherMetricsTest {

    private val objectMapper = jacksonObjectMapper()
    private val config = ScimServerConfig()
    private val users = mutableMapOf<String, User>()
    private val metrics = mockk<ScimMetrics>(relaxed = true)

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

    private val userHandler = object : ResourceHandler<User> {
        override val resourceType: Class<User> = User::class.java
        override val endpoint: String = "/Users"

        override fun get(id: String, context: ScimRequestContext): User = users[id] ?: throw ResourceNotFoundException("User not found: $id")

        override fun create(resource: User, context: ScimRequestContext): User {
            val id = UUID.randomUUID().toString()
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
            return existing
        }

        override fun delete(id: String, version: String?, context: ScimRequestContext) {
            if (!users.containsKey(id)) throw ResourceNotFoundException("User not found: $id")
            users.remove(id)
        }

        override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> = ListResponse(totalResults = users.size, resources = users.values.toList())
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
            serializer = serializer,
            metrics = metrics,
        )
    }

    private fun createTestUser(): User {
        val id = UUID.randomUUID().toString()
        val user = User(id = id, userName = "user-$id")
        users[id] = user
        return user
    }

    @Test
    fun `GET by id calls recordOperation with status 200`() {
        val user = createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/${user.id}",
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        verify { metrics.recordOperation("/Users", "GET", 200, any<Duration>()) }
    }

    @Test
    fun `POST calls recordOperation with status 201`() {
        val body = objectMapper.writeValueAsBytes(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to "newuser"),
        )
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body,
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 201
        verify { metrics.recordOperation("/Users", "POST", 201, any<Duration>()) }
    }

    @Test
    fun `GET 404 calls recordOperation with status 404`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent",
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 404
        verify { metrics.recordOperation("/Users", "GET", 404, any<Duration>()) }
    }

    @Test
    fun `search calls recordSearchResults with correct totalResults`() {
        createTestUser()
        createTestUser()
        createTestUser()

        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        verify { metrics.recordSearchResults("/Users", 3, any<Duration>()) }
    }

    @Test
    fun `POST dot search calls recordSearchResults with correct totalResults`() {
        createTestUser()
        createTestUser()

        val searchRequest = SearchRequest(filter = "userName eq \"test\"")
        val body = objectMapper.writeValueAsBytes(searchRequest)
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users/.search",
            body = body,
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        verify { metrics.recordSearchResults("/Users", 2, any<Duration>()) }
    }

    @Test
    fun `PATCH calls recordPatchOperations with correct operation count`() {
        val user = createTestUser()
        val patchRequest = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = objectMapper.valueToTree("New Name")),
                PatchOperation(op = PatchOp.REPLACE, path = "nickName", value = objectMapper.valueToTree("Nick")),
                PatchOperation(op = PatchOp.ADD, path = "title", value = objectMapper.valueToTree("Eng")),
            ),
        )
        val body = objectMapper.writeValueAsBytes(patchRequest)
        val request = ScimHttpRequest(
            method = HttpMethod.PATCH,
            path = "${config.basePath}/Users/${user.id}",
            body = body,
        )

        val response = dispatcher.dispatch(request)

        response.status shouldBe 200
        verify { metrics.recordPatchOperations("/Users", 3, any<Duration>()) }
    }

    @Test
    fun `active requests incremented on start and decremented on completion`() {
        val user = createTestUser()
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/${user.id}",
        )

        dispatcher.dispatch(request)

        verify(exactly = 1) { metrics.incrementActiveRequests("/Users") }
        verify(exactly = 1) { metrics.decrementActiveRequests("/Users") }
    }

    @Test
    fun `active requests decremented even when handler throws exception`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/nonexistent",
        )

        dispatcher.dispatch(request)

        verify(exactly = 1) { metrics.incrementActiveRequests("/Users") }
        verify(exactly = 1) { metrics.decrementActiveRequests("/Users") }
    }
}
