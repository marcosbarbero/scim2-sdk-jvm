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
package com.marcosbarbero.scim2.server.adapter.discovery

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.port.ResourceHandler
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DiscoveryServiceTest {

    private val config = ScimServerConfig(
        patchEnabled = true,
        bulkEnabled = true,
        bulkMaxOperations = 500,
        bulkMaxPayloadSize = 2_000_000,
        filterEnabled = true,
        filterMaxResults = 100,
        sortEnabled = false,
        etagEnabled = true,
        changePasswordEnabled = false
    )

    private val schemaRegistry = SchemaRegistry()
    private val userHandler = mockk<ResourceHandler<User>> {
        every { endpoint } returns "/Users"
        every { resourceType } returns User::class.java
    }
    private val groupHandler = mockk<ResourceHandler<Group>> {
        every { endpoint } returns "/Groups"
        every { resourceType } returns Group::class.java
    }

    private lateinit var discoveryService: DiscoveryService

    @BeforeEach
    fun setup() {
        schemaRegistry.register(User::class)
        schemaRegistry.register(Group::class)
        discoveryService = DiscoveryService(listOf(userHandler, groupHandler), schemaRegistry, config)
    }

    @Test
    fun `getServiceProviderConfig should reflect config settings`() {
        val spc = discoveryService.getServiceProviderConfig()

        spc.patch.supported shouldBe true
        spc.bulk.supported shouldBe true
        spc.bulk.maxOperations shouldBe 500
        spc.bulk.maxPayloadSize shouldBe 2_000_000
        spc.filter.supported shouldBe true
        spc.filter.maxResults shouldBe 100
        spc.sort.supported shouldBe false
        spc.etag.supported shouldBe true
        spc.changePassword.supported shouldBe false
    }

    @Test
    fun `getSchemas should return all registered schemas`() {
        val result = discoveryService.getSchemas()

        result.totalResults shouldBe 2
        result.resources shouldHaveSize 2
    }

    @Test
    fun `getSchema should return schema by id`() {
        val schema = discoveryService.getSchema(ScimUrns.USER)

        schema.id shouldBe ScimUrns.USER
    }

    @Test
    fun `getSchema should throw ResourceNotFoundException for unknown schema`() {
        assertThrows<ResourceNotFoundException> {
            discoveryService.getSchema("urn:unknown")
        }
    }

    @Test
    fun `getResourceTypes should return all registered resource types`() {
        val result = discoveryService.getResourceTypes()

        result.totalResults shouldBe 2
        result.resources shouldHaveSize 2
    }

    @Test
    fun `getResourceType should return resource type by name`() {
        val rt = discoveryService.getResourceType("User")

        rt.name shouldBe "User"
        rt.endpoint shouldBe "/Users"
    }

    @Test
    fun `getResourceType should throw ResourceNotFoundException for unknown type`() {
        assertThrows<ResourceNotFoundException> {
            discoveryService.getResourceType("Unknown")
        }
    }
}
