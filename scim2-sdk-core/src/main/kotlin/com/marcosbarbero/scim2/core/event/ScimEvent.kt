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
