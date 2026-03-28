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
package com.marcosbarbero.scim2.server.provisioning

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

class ScimOutboundEventPublisherTest {

    private val target = mockk<ScimOutboundTarget>(relaxed = true)
    private val userHandler = mockk<ResourceHandler<User>>()
    private lateinit var publisher: ScimOutboundEventPublisher

    private val testUser = User(id = "user-123", userName = "bjensen")

    @BeforeEach
    fun setup() {
        every { userHandler.resourceType } returns User::class.java
        every { userHandler.endpoint } returns "/Users"
        every { userHandler.get(eq("user-123"), any<ScimRequestContext>()) } returns testUser

        publisher = ScimOutboundEventPublisher(target, listOf(userHandler))
    }

    @Test
    fun `should push created resource to target`() {
        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "user-123"))

        verify { target.create("/Users", testUser) }
    }

    @Test
    fun `should push replaced resource to target`() {
        publisher.publish(ResourceReplacedEvent(resourceType = "User", resourceId = "user-123"))

        verify { target.replace("/Users", "user-123", testUser) }
    }

    @Test
    fun `should push patched resource as replace to target`() {
        publisher.publish(ResourcePatchedEvent(resourceType = "User", resourceId = "user-123", operationCount = 1))

        verify { target.replace("/Users", "user-123", testUser) }
    }

    @Test
    fun `should push delete to target`() {
        publisher.publish(ResourceDeletedEvent(resourceType = "User", resourceId = "user-123"))

        verify { target.delete("/Users", "user-123") }
    }

    @Test
    fun `should ignore bulk events`() {
        publisher.publish(BulkOperationCompletedEvent(operationCount = 5))

        verify(exactly = 0) { target.create(any(), any()) }
        verify(exactly = 0) { target.replace(any(), any(), any()) }
        verify(exactly = 0) { target.delete(any(), any()) }
    }

    @Test
    fun `should skip when handler not found`() {
        publisher.publish(ResourceCreatedEvent(resourceType = "UnknownType", resourceId = "123"))

        verify(exactly = 0) { target.create(any(), any()) }
    }

    @Test
    fun `should not propagate exceptions from target`() {
        every { target.create(any(), any()) } throws RuntimeException("Connection refused")

        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "user-123"))
        // No exception thrown
    }
}
