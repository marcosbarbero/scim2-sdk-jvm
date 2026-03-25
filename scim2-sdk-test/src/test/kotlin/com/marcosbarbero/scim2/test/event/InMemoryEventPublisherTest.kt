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
package com.marcosbarbero.scim2.test.event

import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class InMemoryEventPublisherTest {

    private val publisher = InMemoryEventPublisher()

    @Test
    fun `should start with empty events`() {
        publisher.events.shouldBeEmpty()
    }

    @Test
    fun `should capture published events`() {
        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "1")

        publisher.publish(event)

        publisher.events shouldHaveSize 1
        publisher.events[0].shouldBeInstanceOf<ResourceCreatedEvent>()
        publisher.events[0].resourceId shouldBe "1"
    }

    @Test
    fun `should capture multiple events in order`() {
        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "1"))
        publisher.publish(ResourceReplacedEvent(resourceType = "User", resourceId = "1"))
        publisher.publish(ResourceDeletedEvent(resourceType = "User", resourceId = "1"))

        publisher.events shouldHaveSize 3
        publisher.events[0].shouldBeInstanceOf<ResourceCreatedEvent>()
        publisher.events[1].shouldBeInstanceOf<ResourceReplacedEvent>()
        publisher.events[2].shouldBeInstanceOf<ResourceDeletedEvent>()
    }

    @Test
    fun `clear should remove all events`() {
        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "1"))
        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "2"))

        publisher.clear()

        publisher.events.shouldBeEmpty()
    }

    @Test
    fun `events list should be a defensive copy`() {
        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "1"))
        val snapshot = publisher.events

        publisher.publish(ResourceCreatedEvent(resourceType = "User", resourceId = "2"))

        snapshot shouldHaveSize 1
        publisher.events shouldHaveSize 2
    }
}
