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
package com.marcosbarbero.scim2.sample.plain

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
import java.net.InetSocketAddress
import java.net.URI

/**
 * Minimal SCIM 2.0 server without Spring Boot.
 * Demonstrates how to use the SDK with just the JDK's built-in HTTP server.
 */
fun main() {
    val config = ScimServerConfig(basePath = "/scim/v2")
    val serializer = JacksonScimSerializer()

    // 1. Create in-memory repositories and handlers
    val userRepository = InMemoryResourceRepository<User> { user, id, meta ->
        user.copy(id = id, meta = meta)
    }
    val groupRepository = InMemoryResourceRepository<Group> { group, id, meta ->
        group.copy(id = id, meta = meta)
    }

    val userHandler = InMemoryResourceHandler(
        resourceType = User::class.java,
        endpoint = "/Users",
        repository = userRepository,
    )
    val groupHandler = InMemoryResourceHandler(
        resourceType = Group::class.java,
        endpoint = "/Groups",
        repository = groupRepository,
    )

    // 2. Create schema registry
    val schemaRegistry = SchemaRegistry().apply {
        register(User::class)
        register(Group::class)
    }

    // 3. Create discovery service and dispatcher
    val discoveryService = DiscoveryService(
        handlers = listOf(userHandler, groupHandler),
        schemaRegistry = schemaRegistry,
        config = config,
    )
    val dispatcher = ScimEndpointDispatcher(
        handlers = listOf(userHandler, groupHandler),
        bulkHandler = null,
        meHandler = null,
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
    )

    // 4. Start JDK HTTP server
    val server = HttpServer.create(InetSocketAddress(8080), 0)
    server.createContext("/scim/v2") { exchange ->
        val scimRequest = exchange.toScimHttpRequest()
        val scimResponse = dispatcher.dispatch(scimRequest)

        // Write response headers
        scimResponse.headers.forEach { (key, value) ->
            exchange.responseHeaders.add(key, value)
        }

        // Write response body
        val responseBody = scimResponse.body ?: ByteArray(0)
        exchange.sendResponseHeaders(scimResponse.status, if (responseBody.isEmpty()) -1 else responseBody.size.toLong())
        if (responseBody.isNotEmpty()) {
            exchange.responseBody.use { it.write(responseBody) }
        }
    }
    server.executor = null
    server.start()
    println("SCIM server started on http://localhost:8080/scim/v2")
    println("Try: curl http://localhost:8080/scim/v2/ServiceProviderConfig")
}

/**
 * Bridges JDK [HttpExchange] to SDK [ScimHttpRequest].
 */
private fun HttpExchange.toScimHttpRequest(): ScimHttpRequest {
    val method = HttpMethod.valueOf(requestMethod.uppercase())
    val path = requestURI.path

    // Convert headers
    val headers: Map<String, List<String>> = requestHeaders.toMap()

    // Parse query parameters
    val queryParameters = parseQueryParameters(requestURI)

    // Read request body
    val body = requestBody.use { it.readAllBytes() }.takeIf { it.isNotEmpty() }

    return ScimHttpRequest(
        method = method,
        path = path,
        headers = headers,
        queryParameters = queryParameters,
        body = body,
    )
}

/**
 * Parses query parameters from a [URI].
 */
private fun parseQueryParameters(uri: URI): Map<String, List<String>> {
    val query = uri.rawQuery ?: return emptyMap()
    val result = mutableMapOf<String, MutableList<String>>()
    query.split("&").forEach { param ->
        val parts = param.split("=", limit = 2)
        val key = java.net.URLDecoder.decode(parts[0], Charsets.UTF_8)
        val value = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], Charsets.UTF_8) else ""
        result.getOrPut(key) { mutableListOf() }.add(value)
    }
    return result
}
