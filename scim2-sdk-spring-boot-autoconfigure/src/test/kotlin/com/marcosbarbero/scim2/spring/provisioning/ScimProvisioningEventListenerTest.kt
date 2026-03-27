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

class ScimProvisioningEventListenerTest {

    private val client = mockk<ScimClient>(relaxed = true)
    private val userHandler = mockk<ResourceHandler<User>>()
    private lateinit var listener: ScimProvisioningEventListener

    private val testUser = User(id = "user-123", userName = "bjensen")

    @BeforeEach
    fun setup() {
        every { userHandler.resourceType } returns User::class.java
        every { userHandler.endpoint } returns "/Users"
        every { userHandler.get(eq("user-123"), any<ScimRequestContext>()) } returns testUser
        every { client.create(any(), any<User>(), any<KClass<User>>()) } returns ScimResponse(201, testUser)
        every { client.replace(any(), any(), any<User>(), any<KClass<User>>()) } returns ScimResponse(200, testUser)

        listener = ScimProvisioningEventListener(client, listOf(userHandler))
    }

    @Test
    fun `should provision created resource to target`() {
        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "user-123")

        listener.onScimEvent(event)

        verify { userHandler.get("user-123", any<ScimRequestContext>()) }
        verify { client.create("/Users", testUser, User::class) }
    }

    @Test
    fun `should provision replaced resource to target`() {
        val event = ResourceReplacedEvent(resourceType = "User", resourceId = "user-123")

        listener.onScimEvent(event)

        verify { userHandler.get("user-123", any<ScimRequestContext>()) }
        verify { client.replace("/Users", "user-123", testUser, User::class) }
    }

    @Test
    fun `should provision patched resource as replace to target`() {
        val event = ResourcePatchedEvent(resourceType = "User", resourceId = "user-123", operationCount = 1)

        listener.onScimEvent(event)

        verify { userHandler.get("user-123", any<ScimRequestContext>()) }
        verify { client.replace("/Users", "user-123", testUser, User::class) }
    }

    @Test
    fun `should provision deleted resource to target`() {
        val event = ResourceDeletedEvent(resourceType = "User", resourceId = "user-123")

        listener.onScimEvent(event)

        verify { client.delete("/Users", "user-123") }
    }

    @Test
    fun `should ignore bulk events`() {
        val event = BulkOperationCompletedEvent(operationCount = 5)

        listener.onScimEvent(event)

        verify(exactly = 0) { client.create(any(), any<User>(), any<KClass<User>>()) }
        verify(exactly = 0) { client.delete(any(), any()) }
    }

    @Test
    fun `should not fail when handler not found for resource type`() {
        val event = ResourceCreatedEvent(resourceType = "UnknownType", resourceId = "123")

        listener.onScimEvent(event)

        verify(exactly = 0) { client.create(any(), any<User>(), any<KClass<User>>()) }
    }

    @Test
    fun `should not fail when client throws exception`() {
        every { client.create(any(), any<User>(), any<KClass<User>>()) } throws RuntimeException("Connection refused")

        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "user-123")

        listener.onScimEvent(event)
    }
}
