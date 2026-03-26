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
package com.marcosbarbero.scim2.test.server

import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository

class InMemoryScimServer(
    val config: ScimServerConfig = ScimServerConfig(),
    val serializer: ScimSerializer = JacksonScimSerializer(),
) {
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

    private val schemaRegistry = SchemaRegistry().apply {
        register(User::class)
        register(Group::class)
    }

    private val discoveryService = DiscoveryService(
        handlers = listOf(userHandler, groupHandler),
        schemaRegistry = schemaRegistry,
        config = config,
    )

    private val dispatcher = ScimEndpointDispatcher(
        handlers = listOf(userHandler, groupHandler),
        bulkHandler = null,
        meHandler = null,
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
    )

    fun dispatch(request: ScimHttpRequest): ScimHttpResponse = dispatcher.dispatch(request)

    fun createUser(user: User): ScimHttpResponse {
        val body = serializer.serialize(user)
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Users",
            body = body,
        )
        return dispatch(request)
    }

    fun getUser(id: String): ScimHttpResponse {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users/$id",
        )
        return dispatch(request)
    }

    fun replaceUser(id: String, user: User): ScimHttpResponse {
        val body = serializer.serialize(user)
        val request = ScimHttpRequest(
            method = HttpMethod.PUT,
            path = "${config.basePath}/Users/$id",
            body = body,
        )
        return dispatch(request)
    }

    fun deleteUser(id: String): ScimHttpResponse {
        val request = ScimHttpRequest(
            method = HttpMethod.DELETE,
            path = "${config.basePath}/Users/$id",
        )
        return dispatch(request)
    }

    fun searchUsers(filter: String? = null, startIndex: Int? = null, count: Int? = null): ScimHttpResponse {
        val queryParams = mutableMapOf<String, List<String>>()
        filter?.let { queryParams["filter"] = listOf(it) }
        startIndex?.let { queryParams["startIndex"] = listOf(it.toString()) }
        count?.let { queryParams["count"] = listOf(it.toString()) }
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Users",
            queryParameters = queryParams,
        )
        return dispatch(request)
    }

    fun createGroup(group: Group): ScimHttpResponse {
        val body = serializer.serialize(group)
        val request = ScimHttpRequest(
            method = HttpMethod.POST,
            path = "${config.basePath}/Groups",
            body = body,
        )
        return dispatch(request)
    }

    fun getGroup(id: String): ScimHttpResponse {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${config.basePath}/Groups/$id",
        )
        return dispatch(request)
    }

    fun deleteGroup(id: String): ScimHttpResponse {
        val request = ScimHttpRequest(
            method = HttpMethod.DELETE,
            path = "${config.basePath}/Groups/$id",
        )
        return dispatch(request)
    }

    fun reset() {
        userRepository.clear()
        groupRepository.clear()
    }
}
