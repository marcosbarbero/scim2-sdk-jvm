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
package com.marcosbarbero.scim2.spring.provisioning

import com.marcosbarbero.scim2.client.api.ScimClient
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourcePatchedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import kotlin.reflect.KClass

/**
 * Listens for SCIM resource mutation events and provisions them outbound
 * to a remote SCIM server via the configured [ScimClient].
 *
 * This enables bidirectional sync: when a resource is created/updated/deleted
 * locally, the change is automatically pushed to the target SCIM endpoint
 * (e.g., Keycloak with SCIM extension, another SCIM server, etc.).
 *
 * Authentication to the target is handled by the [com.marcosbarbero.scim2.client.port.AuthenticationStrategy]
 * bean configured by the user — no credentials are stored in config properties.
 *
 * Auto-configured when both [ScimClient] and [ResourceHandler] beans are present
 * (i.e., when `scim.client.base-url` is set).
 */
class ScimOutboundProvisioningListener(
    private val client: ScimClient,
    private val handlers: List<ResourceHandler<*>>,
) {
    private val log = LoggerFactory.getLogger(ScimOutboundProvisioningListener::class.java)

    @EventListener
    fun onScimEvent(event: ScimEvent) {
        try {
            when (event) {
                is ResourceCreatedEvent -> handleCreated(event)
                is ResourceReplacedEvent -> handleReplaced(event)
                is ResourcePatchedEvent -> handleReplaced(event)
                is ResourceDeletedEvent -> handleDeleted(event)
                else -> log.debug("Ignoring event type: {}", event::class.simpleName)
            }
        } catch (e: Exception) {
            log.error(
                "Outbound provisioning failed for {} {}: {}",
                event.resourceType,
                event.resourceId,
                e.message,
                e,
            )
        }
    }

    private fun handleCreated(event: ResourceCreatedEvent) {
        val handler = findHandler(event.resourceType) ?: return
        val resource = fetchResource(handler, event.resourceId)
        log.info("Outbound CREATE {} {} to {}", event.resourceType, event.resourceId, handler.endpoint)
        createResource(handler, resource)
    }

    private fun handleReplaced(event: ScimEvent) {
        val handler = findHandler(event.resourceType) ?: return
        val resource = fetchResource(handler, event.resourceId)
        log.info("Outbound REPLACE {} {} to {}", event.resourceType, event.resourceId, handler.endpoint)
        replaceResource(handler, event.resourceId, resource)
    }

    private fun handleDeleted(event: ResourceDeletedEvent) {
        val handler = findHandler(event.resourceType) ?: return
        log.info("Outbound DELETE {} {} from {}", event.resourceType, event.resourceId, handler.endpoint)
        client.delete(handler.endpoint, event.resourceId)
    }

    private fun findHandler(resourceType: String): ResourceHandler<*>? {
        val handler = handlers.firstOrNull { it.resourceType.simpleName == resourceType }
        if (handler == null) {
            log.warn("No handler found for resource type '{}', skipping outbound provisioning", resourceType)
        }
        return handler
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ScimResource> fetchResource(handler: ResourceHandler<*>, id: String): T {
        val context = ScimRequestContext(principalId = "scim-outbound-provisioning", roles = setOf("admin"))
        return (handler as ResourceHandler<T>).get(id, context)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ScimResource> createResource(handler: ResourceHandler<*>, resource: T) {
        client.create(handler.endpoint, resource, handler.resourceType.kotlin as KClass<T>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ScimResource> replaceResource(handler: ResourceHandler<*>, id: String, resource: T) {
        client.replace(handler.endpoint, id, resource, handler.resourceType.kotlin as KClass<T>)
    }
}
