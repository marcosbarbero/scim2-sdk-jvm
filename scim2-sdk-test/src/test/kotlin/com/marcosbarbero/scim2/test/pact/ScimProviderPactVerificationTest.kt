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
package com.marcosbarbero.scim2.test.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress
import java.net.URI

/**
 * Pact Provider verification test.
 *
 * Verifies that our SCIM server implementation satisfies the contracts
 * defined by the ScimClient consumer tests.
 *
 * The pact files are loaded from the client module's target/pacts directory.
 * This test is skipped when pact files do not exist (consumer tests must run first).
 */
@Provider("ScimServiceProvider")
@PactFolder("../../scim2-sdk-client/target/pacts")
@EnabledIf("pactFilesExist")
class ScimProviderPactVerificationTest {

    companion object {
        private var serverPort: Int = 0
        private lateinit var httpServer: HttpServer
        private lateinit var userRepository: InMemoryResourceRepository<User>
        private lateinit var groupRepository: InMemoryResourceRepository<Group>
        private lateinit var dispatcher: ScimEndpointDispatcher
        private val serializer = JacksonScimSerializer()

        @JvmStatic
        fun pactFilesExist(): Boolean {
            val pactDir = java.io.File("../../scim2-sdk-client/target/pacts")
            // Also try from the module directory
            val altPactDir = java.io.File(
                System.getProperty("user.dir") + "/../../scim2-sdk-client/target/pacts"
            )
            return (pactDir.isDirectory && pactDir.listFiles()?.isNotEmpty() == true) ||
                (altPactDir.isDirectory && altPactDir.listFiles()?.isNotEmpty() == true)
        }

        @BeforeAll
        @JvmStatic
        fun startServer() {
            val config = ScimServerConfig()

            userRepository = InMemoryResourceRepository { user, id, meta ->
                user.copy(id = id, meta = meta)
            }
            groupRepository = InMemoryResourceRepository { group, id, meta ->
                group.copy(id = id, meta = meta)
            }

            val userHandler = InMemoryResourceHandler(
                resourceType = User::class.java,
                endpoint = "/Users",
                repository = userRepository
            )
            val groupHandler = InMemoryResourceHandler(
                resourceType = Group::class.java,
                endpoint = "/Groups",
                repository = groupRepository
            )

            val schemaRegistry = SchemaRegistry().apply {
                register(User::class)
                register(Group::class)
            }

            val discoveryService = DiscoveryService(
                handlers = listOf(userHandler, groupHandler),
                schemaRegistry = schemaRegistry,
                config = config
            )

            dispatcher = ScimEndpointDispatcher(
                handlers = listOf(userHandler, groupHandler),
                bulkHandler = null,
                meHandler = null,
                discoveryService = discoveryService,
                config = config,
                serializer = serializer
            )

            httpServer = HttpServer.create(InetSocketAddress(0), 0)
            httpServer.createContext("/") { exchange -> handleRequest(exchange) }
            httpServer.start()
            serverPort = httpServer.address.port
        }

        @JvmStatic
        fun stopServer() {
            if (::httpServer.isInitialized) {
                httpServer.stop(0)
            }
        }

        private fun handleRequest(exchange: HttpExchange) {
            try {
                val uri = exchange.requestURI
                val method = HttpMethod.valueOf(exchange.requestMethod.uppercase())
                val headers = exchange.requestHeaders.mapValues { (_, values) -> values }
                val queryParams = parseQueryParams(uri)
                val body = exchange.requestBody.readBytes().takeIf { it.isNotEmpty() }

                val scimRequest = ScimHttpRequest(
                    method = method,
                    path = uri.path,
                    headers = headers,
                    queryParameters = queryParams,
                    body = body
                )

                val scimResponse = dispatcher.dispatch(scimRequest)

                scimResponse.headers.forEach { (key, value) ->
                    exchange.responseHeaders.add(key, value)
                }

                val responseBody = scimResponse.body
                if (responseBody != null && responseBody.isNotEmpty()) {
                    exchange.sendResponseHeaders(scimResponse.status, responseBody.size.toLong())
                    exchange.responseBody.write(responseBody)
                } else {
                    exchange.sendResponseHeaders(scimResponse.status, -1)
                }
                exchange.close()
            } catch (e: Exception) {
                val errorBody = """{"schemas":["urn:ietf:params:scim:api:messages:2.0:Error"],"status":"500","detail":"${e.message}"}""".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/scim+json")
                exchange.sendResponseHeaders(500, errorBody.size.toLong())
                exchange.responseBody.write(errorBody)
                exchange.close()
            }
        }

        private fun parseQueryParams(uri: URI): Map<String, List<String>> {
            val query = uri.query ?: return emptyMap()
            return query.split("&")
                .map { it.split("=", limit = 2) }
                .filter { it.size == 2 }
                .groupBy({ it[0] }, { java.net.URLDecoder.decode(it[1], Charsets.UTF_8) })
        }
    }

    @BeforeEach
    fun setUp(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", serverPort)
    }

    @State("no users exist")
    fun noUsersExist() {
        userRepository.clear()
        groupRepository.clear()
    }

    @State("a user with id 123 exists")
    fun userExists() {
        userRepository.clear()
        val user = User(userName = "pact.test.user")
        val created = userRepository.create(user)
        // The InMemoryResourceRepository assigns a UUID, but the pact expects id "123".
        // We need to ensure the user can be fetched by its actual ID.
        // The pact consumer uses stringValue("id", "123") for GET, meaning it expects exact match.
        // We store with explicit id "123".
        userRepository.clear()
        userRepository.createWithId("123", user)
    }

    @State("no user with id 999 exists")
    fun userDoesNotExist() {
        // Ensure no user with id 999 exists - clear to be safe
        userRepository.deleteIfExists("999")
    }

    @State("users exist")
    fun usersExist() {
        userRepository.clear()
        userRepository.create(User(userName = "pact.user.1"))
        userRepository.create(User(userName = "pact.user.2"))
    }

    @State("server is running")
    fun serverRunning() {
        // No-op -- server is already running
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext) {
        context.verifyInteraction()
    }
}
