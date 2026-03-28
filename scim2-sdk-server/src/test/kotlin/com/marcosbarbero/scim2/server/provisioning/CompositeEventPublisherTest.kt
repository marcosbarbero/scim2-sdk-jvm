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

import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

class CompositeEventPublisherTest {

    @Test
    fun `should publish to all delegates`() {
        val delegate1 = mockk<ScimEventPublisher>(relaxed = true)
        val delegate2 = mockk<ScimEventPublisher>(relaxed = true)
        val composite = CompositeEventPublisher(delegate1, delegate2)

        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "123")
        composite.publish(event)

        verify { delegate1.publish(event) }
        verify { delegate2.publish(event) }
    }

    @Test
    fun `should continue publishing when a delegate fails`() {
        val failing = mockk<ScimEventPublisher>()
        val succeeding = mockk<ScimEventPublisher>(relaxed = true)
        every { failing.publish(any()) } throws RuntimeException("Failed")

        val composite = CompositeEventPublisher(failing, succeeding)
        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "123")
        composite.publish(event)

        verify { succeeding.publish(event) }
    }

    @Test
    fun `should publish in order`() {
        val first = mockk<ScimEventPublisher>(relaxed = true)
        val second = mockk<ScimEventPublisher>(relaxed = true)
        val composite = CompositeEventPublisher(first, second)

        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "123")
        composite.publish(event)

        verifyOrder {
            first.publish(event)
            second.publish(event)
        }
    }

    @Test
    fun `should work with list constructor`() {
        val delegate = mockk<ScimEventPublisher>(relaxed = true)
        val composite = CompositeEventPublisher(listOf(delegate))

        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "123")
        composite.publish(event)

        verify { delegate.publish(event) }
    }
}
