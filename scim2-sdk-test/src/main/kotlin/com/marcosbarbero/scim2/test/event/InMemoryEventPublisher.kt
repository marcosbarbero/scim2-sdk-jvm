package com.marcosbarbero.scim2.test.event

import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher

class InMemoryEventPublisher : ScimEventPublisher {
    private val _events = mutableListOf<ScimEvent>()
    val events: List<ScimEvent> get() = _events.toList()

    override fun publish(event: ScimEvent) { _events.add(event) }
    fun clear() { _events.clear() }
}
