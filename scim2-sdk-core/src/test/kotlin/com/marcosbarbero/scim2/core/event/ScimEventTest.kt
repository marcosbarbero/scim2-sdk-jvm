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
package com.marcosbarbero.scim2.core.event

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.Instant

class ScimEventTest {

    @Test
    fun `ResourceCreatedEvent should have correct defaults`() {
        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "123")

        event.eventId.shouldNotBeEmpty()
        event.timestamp shouldNotBe null
        event.resourceType shouldBe "User"
        event.resourceId shouldBe "123"
        event.correlationId shouldBe null
    }

    @Test
    fun `ResourceReplacedEvent should have correct defaults`() {
        val event = ResourceReplacedEvent(resourceType = "User", resourceId = "456")

        event.eventId.shouldNotBeEmpty()
        event.timestamp shouldNotBe null
        event.resourceType shouldBe "User"
        event.resourceId shouldBe "456"
        event.correlationId shouldBe null
    }

    @Test
    fun `ResourcePatchedEvent should have correct defaults`() {
        val event = ResourcePatchedEvent(resourceType = "Group", resourceId = "789")

        event.eventId.shouldNotBeEmpty()
        event.operationCount shouldBe 0
        event.correlationId shouldBe null
    }

    @Test
    fun `ResourcePatchedEvent should accept operationCount`() {
        val event = ResourcePatchedEvent(resourceType = "User", resourceId = "1", operationCount = 3)

        event.operationCount shouldBe 3
    }

    @Test
    fun `ResourceDeletedEvent should have correct defaults`() {
        val event = ResourceDeletedEvent(resourceType = "User", resourceId = "del-1")

        event.eventId.shouldNotBeEmpty()
        event.resourceType shouldBe "User"
        event.resourceId shouldBe "del-1"
        event.correlationId shouldBe null
    }

    @Test
    fun `BulkOperationCompletedEvent should have correct defaults`() {
        val event = BulkOperationCompletedEvent()

        event.resourceType shouldBe "Bulk"
        event.resourceId shouldBe ""
        event.operationCount shouldBe 0
        event.failureCount shouldBe 0
    }

    @Test
    fun `BulkOperationCompletedEvent should accept counts`() {
        val event = BulkOperationCompletedEvent(operationCount = 5, failureCount = 2)

        event.operationCount shouldBe 5
        event.failureCount shouldBe 2
    }

    @Test
    fun `all events should accept custom correlationId`() {
        val corrId = "corr-abc-123"

        val created = ResourceCreatedEvent(resourceType = "User", resourceId = "1", correlationId = corrId)
        val replaced = ResourceReplacedEvent(resourceType = "User", resourceId = "1", correlationId = corrId)
        val patched = ResourcePatchedEvent(resourceType = "User", resourceId = "1", correlationId = corrId)
        val deleted = ResourceDeletedEvent(resourceType = "User", resourceId = "1", correlationId = corrId)
        val bulk = BulkOperationCompletedEvent(correlationId = corrId)

        created.correlationId shouldBe corrId
        replaced.correlationId shouldBe corrId
        patched.correlationId shouldBe corrId
        deleted.correlationId shouldBe corrId
        bulk.correlationId shouldBe corrId
    }

    @Test
    fun `sealed class hierarchy is exhaustive via when expression`() {
        val events: List<ScimEvent> = listOf(
            ResourceCreatedEvent(resourceType = "User", resourceId = "1"),
            ResourceReplacedEvent(resourceType = "User", resourceId = "2"),
            ResourcePatchedEvent(resourceType = "User", resourceId = "3"),
            ResourceDeletedEvent(resourceType = "User", resourceId = "4"),
            BulkOperationCompletedEvent()
        )

        val results = events.map { event ->
            when (event) {
                is ResourceCreatedEvent -> "created"
                is ResourceReplacedEvent -> "replaced"
                is ResourcePatchedEvent -> "patched"
                is ResourceDeletedEvent -> "deleted"
                is BulkOperationCompletedEvent -> "bulk"
            }
        }

        results shouldBe listOf("created", "replaced", "patched", "deleted", "bulk")
    }

    @Test
    fun `all event subtypes are instances of ScimEvent`() {
        ResourceCreatedEvent(resourceType = "User", resourceId = "1").shouldBeInstanceOf<ScimEvent>()
        ResourceReplacedEvent(resourceType = "User", resourceId = "1").shouldBeInstanceOf<ScimEvent>()
        ResourcePatchedEvent(resourceType = "User", resourceId = "1").shouldBeInstanceOf<ScimEvent>()
        ResourceDeletedEvent(resourceType = "User", resourceId = "1").shouldBeInstanceOf<ScimEvent>()
        BulkOperationCompletedEvent().shouldBeInstanceOf<ScimEvent>()
    }

    @Test
    fun `events should accept custom eventId and timestamp`() {
        val customId = "custom-id-123"
        val customTimestamp = Instant.parse("2025-01-01T00:00:00Z")

        val event = ResourceCreatedEvent(
            eventId = customId,
            timestamp = customTimestamp,
            resourceType = "User",
            resourceId = "1"
        )

        event.eventId shouldBe customId
        event.timestamp shouldBe customTimestamp
    }
}
