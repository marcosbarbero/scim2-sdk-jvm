package com.marcosbarbero.scim2.core.event

import org.junit.jupiter.api.Test

class ScimEventPublisherTest {

    @Test
    fun `NoOpEventPublisher should not throw on publish`() {
        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "1")
        NoOpEventPublisher.publish(event)
    }
}
