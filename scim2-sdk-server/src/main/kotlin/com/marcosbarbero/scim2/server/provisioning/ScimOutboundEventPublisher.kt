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

import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourcePatchedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import org.slf4j.LoggerFactory

/**
 * Framework-agnostic [ScimEventPublisher] that pushes resource mutations to an
 * outbound [ScimOutboundTarget]. Works without Spring — can be used with any
 * JVM HTTP framework.
 *
 * ```java
 * // Wire into the dispatcher
 * var publisher = new ScimOutboundEventPublisher(target, handlers);
 * var dispatcher = new ScimEndpointDispatcher(
 *     handlers, ..., eventPublisher = publisher, ...
 * );
 * ```
 *
 * For Spring users, the auto-configured [com.marcosbarbero.scim2.spring.provisioning.ScimOutboundProvisioningListener]
 * is recommended instead, as it integrates with Spring's event system and supports
 * the transactional outbox pattern.
 *
 * @param target the outbound target to push resources to
 * @param handlers the resource handlers used to fetch resources before pushing
 * @see ScimOutboundTarget
 * @see CompositeEventPublisher
 */
class ScimOutboundEventPublisher(
    private val target: ScimOutboundTarget,
    private val handlers: List<ResourceHandler<*>>,
) : ScimEventPublisher {

    private val log = LoggerFactory.getLogger(ScimOutboundEventPublisher::class.java)

    override fun publish(event: ScimEvent) {
        try {
            when (event) {
                is ResourceCreatedEvent -> handleCreated(event)
                is ResourceReplacedEvent -> handleReplaced(event)
                is ResourcePatchedEvent -> handleReplaced(event)
                is ResourceDeletedEvent -> handleDeleted(event)
                else -> log.debug("Ignoring event type: {}", event::class.simpleName)
            }
        } catch (e: Exception) {
            log.error("Outbound provisioning failed for {} {}: {}", event.resourceType, event.resourceId, e.message, e)
        }
    }

    private fun handleCreated(event: ResourceCreatedEvent) {
        val handler = findHandler(event.resourceType) ?: return
        val resource = fetchResource(handler, event.resourceId)
        log.info("Outbound CREATE {} {} to {}", event.resourceType, event.resourceId, handler.endpoint)
        target.create(handler.endpoint, resource)
    }

    private fun handleReplaced(event: ScimEvent) {
        val handler = findHandler(event.resourceType) ?: return
        val resource = fetchResource(handler, event.resourceId)
        log.info("Outbound REPLACE {} {} to {}", event.resourceType, event.resourceId, handler.endpoint)
        target.replace(handler.endpoint, event.resourceId, resource)
    }

    private fun handleDeleted(event: ResourceDeletedEvent) {
        val handler = findHandler(event.resourceType) ?: return
        log.info("Outbound DELETE {} {} from {}", event.resourceType, event.resourceId, handler.endpoint)
        target.delete(handler.endpoint, event.resourceId)
    }

    private fun findHandler(resourceType: String): ResourceHandler<*>? {
        val handler = handlers.firstOrNull { it.resourceType.simpleName == resourceType }
        if (handler == null) {
            log.warn("No handler found for resource type '{}', skipping outbound provisioning", resourceType)
        }
        return handler
    }

    private val provisioningContext = ScimRequestContext(principalId = "scim-outbound-provisioning", roles = setOf("admin"))

    @Suppress("UNCHECKED_CAST")
    private fun fetchResource(handler: ResourceHandler<*>, id: String): ScimResource = (handler as ResourceHandler<ScimResource>).get(id, provisioningContext)
}
