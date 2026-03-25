package com.marcosbarbero.scim2.core.event

interface ScimEventPublisher {
    fun publish(event: ScimEvent)
}

object NoOpEventPublisher : ScimEventPublisher {
    override fun publish(event: ScimEvent) { /* no-op */ }
}
