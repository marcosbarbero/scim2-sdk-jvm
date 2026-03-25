package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.event.ScimEvent

interface ScimOutboxPort {
    fun store(event: ScimEvent)
}
