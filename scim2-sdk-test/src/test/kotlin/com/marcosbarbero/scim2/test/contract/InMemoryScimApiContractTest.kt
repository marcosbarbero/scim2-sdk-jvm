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
package com.marcosbarbero.scim2.test.contract

import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import io.github.serpro69.kfaker.Faker

class InMemoryScimApiContractTest : ScimApiContractTest() {

    private val objectMapper = JacksonScimSerializer.defaultObjectMapper()
    private val faker = Faker()

    override fun createDispatcher(): ScimEndpointDispatcher {
        val userRepository = InMemoryResourceRepository<User> { user, id, meta ->
            user.copy(id = id, meta = meta)
        }
        val groupRepository = InMemoryResourceRepository<Group> { group, id, meta ->
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

        val config = ScimServerConfig()
        val serializer = JacksonScimSerializer()
        val discoveryService = DiscoveryService(
            handlers = listOf(userHandler, groupHandler),
            schemaRegistry = schemaRegistry,
            config = config
        )

        return ScimEndpointDispatcher(
            handlers = listOf(userHandler, groupHandler),
            bulkHandler = null,
            meHandler = null,
            discoveryService = discoveryService,
            config = config,
            serializer = serializer
        )
    }

    override fun sampleUserJson(): ByteArray =
        objectMapper.writeValueAsBytes(
            mapOf(
                "schemas" to listOf("urn:ietf:params:scim:schemas:core:2.0:User"),
                "userName" to faker.internet.email()
            )
        )
}
