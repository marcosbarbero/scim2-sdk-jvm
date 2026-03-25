package com.marcosbarbero.scim2.core.event

import java.time.Instant
import java.util.UUID

sealed class ScimEvent {
    abstract val eventId: String
    abstract val timestamp: Instant
    abstract val resourceType: String
    abstract val resourceId: String
    abstract val correlationId: String?
}

data class ResourceCreatedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val resourceType: String,
    override val resourceId: String,
    override val correlationId: String? = null
) : ScimEvent()

data class ResourceReplacedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val resourceType: String,
    override val resourceId: String,
    override val correlationId: String? = null
) : ScimEvent()

data class ResourcePatchedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val resourceType: String,
    override val resourceId: String,
    override val correlationId: String? = null,
    val operationCount: Int = 0
) : ScimEvent()

data class ResourceDeletedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val resourceType: String,
    override val resourceId: String,
    override val correlationId: String? = null
) : ScimEvent()

data class BulkOperationCompletedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val resourceType: String = "Bulk",
    override val resourceId: String = "",
    override val correlationId: String? = null,
    val operationCount: Int = 0,
    val failureCount: Int = 0
) : ScimEvent()
