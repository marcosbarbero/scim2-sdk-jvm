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
package com.marcosbarbero.scim2.spring.provisioning

import com.marcosbarbero.scim2.client.api.ScimClient
import com.marcosbarbero.scim2.client.api.ScimResponse
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.event.BulkOperationCompletedEvent
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourcePatchedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ScimOutboundProvisioningListenerTest {

    private val client = mockk<ScimClient>(relaxed = true)
    private val userHandler = mockk<ResourceHandler<User>>()
    private lateinit var listener: ScimOutboundProvisioningListener

    private val testUser = User(id = "user-123", userName = "bjensen")

    @BeforeEach
    fun setup() {
        every { userHandler.resourceType } returns User::class.java
        every { userHandler.endpoint } returns "/Users"
        every { userHandler.get(eq("user-123"), any<ScimRequestContext>()) } returns testUser
        every { client.create(any(), any<ScimResource>(), any<KClass<ScimResource>>()) } returns ScimResponse(201, testUser)
        every { client.replace(any(), any(), any<ScimResource>(), any<KClass<ScimResource>>()) } returns ScimResponse(200, testUser)

        listener = ScimOutboundProvisioningListener(client, listOf(userHandler))
    }

    @Test
    fun `should push created resource outbound`() {
        listener.onScimEvent(ResourceCreatedEvent(resourceType = "User", resourceId = "user-123"))

        verify { userHandler.get("user-123", any<ScimRequestContext>()) }
        verify { client.create("/Users", testUser, any<KClass<ScimResource>>()) }
    }

    @Test
    fun `should push replaced resource outbound`() {
        listener.onScimEvent(ResourceReplacedEvent(resourceType = "User", resourceId = "user-123"))

        verify { userHandler.get("user-123", any<ScimRequestContext>()) }
        verify { client.replace("/Users", "user-123", testUser, any<KClass<ScimResource>>()) }
    }

    @Test
    fun `should push patched resource as replace outbound`() {
        listener.onScimEvent(ResourcePatchedEvent(resourceType = "User", resourceId = "user-123", operationCount = 1))

        verify { userHandler.get("user-123", any<ScimRequestContext>()) }
        verify { client.replace("/Users", "user-123", testUser, any<KClass<ScimResource>>()) }
    }

    @Test
    fun `should push delete outbound`() {
        listener.onScimEvent(ResourceDeletedEvent(resourceType = "User", resourceId = "user-123"))

        verify { client.delete("/Users", "user-123") }
    }

    @Test
    fun `should ignore bulk events`() {
        listener.onScimEvent(BulkOperationCompletedEvent(operationCount = 5))

        verify(exactly = 0) { client.create(any(), any<ScimResource>(), any<KClass<ScimResource>>()) }
        verify(exactly = 0) { client.delete(any(), any()) }
    }

    @Test
    fun `should skip when handler not found for resource type`() {
        listener.onScimEvent(ResourceCreatedEvent(resourceType = "UnknownType", resourceId = "123"))

        verify(exactly = 0) { client.create(any(), any<ScimResource>(), any<KClass<ScimResource>>()) }
    }

    @Test
    fun `should not propagate exceptions from client`() {
        every { client.create(any(), any<ScimResource>(), any<KClass<ScimResource>>()) } throws RuntimeException("Connection refused")

        listener.onScimEvent(ResourceCreatedEvent(resourceType = "User", resourceId = "user-123"))
        // No exception thrown — logged and swallowed
    }
}
