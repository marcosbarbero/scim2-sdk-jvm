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
package com.marcosbarbero.scim2.spring.event

import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class SpringScimEventPublisherTest {

    @Test
    fun `should publish ScimEvent as Spring ApplicationEvent`() {
        val captured = slot<Any>()
        val appPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val publisher = SpringScimEventPublisher(appPublisher)

        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "123")
        publisher.publish(event)

        verify { appPublisher.publishEvent(capture(captured)) }
        (captured.captured as ScimEvent).resourceId shouldBe "123"
    }

    @Test
    fun `should publish different event types`() {
        val appPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val publisher = SpringScimEventPublisher(appPublisher)

        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "1"))
        publisher.publish(ResourceDeletedEvent(resourceType = "Group", resourceId = "2"))

        verify(exactly = 2) { appPublisher.publishEvent(any()) }
    }
}
