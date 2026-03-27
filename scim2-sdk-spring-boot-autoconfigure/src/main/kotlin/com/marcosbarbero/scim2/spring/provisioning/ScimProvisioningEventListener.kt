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
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourcePatchedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener

/**
 * Listens for SCIM resource mutation events and provisions them to a remote
 * SCIM server via [ScimClient]. This enables outbound IdP provisioning —
 * when a resource is created/updated/deleted locally, the change is pushed
 * to the configured target.
 */
internal class ScimProvisioningEventListener(
    private val client: ScimClient,
    private val handlers: List<ResourceHandler<*>>,
) {
    private val log = LoggerFactory.getLogger(ScimProvisioningEventListener::class.java)

    @EventListener
    fun onScimEvent(event: ScimEvent) {
        try {
            when (event) {
                is ResourceCreatedEvent -> handleCreated(event)
                is ResourceReplacedEvent -> handleReplaced(event)
                is ResourcePatchedEvent -> handleReplaced(event)
                is ResourceDeletedEvent -> handleDeleted(event)
                else -> log.debug("Ignoring event: {}", event::class.simpleName)
            }
        } catch (e: Exception) {
            log.error("Failed to provision {} {} to target: {}", event.resourceType, event.resourceId, e.message, e)
        }
    }

    private fun handleCreated(event: ResourceCreatedEvent) {
        val handler = findHandler(event.resourceType) ?: return
        val resource = handler.get(event.resourceId, provisioningContext())
        val endpoint = handler.endpoint
        log.info("Provisioning CREATE {} {} to target", event.resourceType, event.resourceId)
        client.create(endpoint, resource, handler.resourceType.kotlin)
    }

    private fun handleReplaced(event: ScimEvent) {
        val handler = findHandler(event.resourceType) ?: return
        val resource = handler.get(event.resourceId, provisioningContext())
        val endpoint = handler.endpoint
        log.info("Provisioning REPLACE {} {} to target", event.resourceType, event.resourceId)
        client.replace(endpoint, event.resourceId, resource, handler.resourceType.kotlin)
    }

    private fun handleDeleted(event: ResourceDeletedEvent) {
        val handler = findHandler(event.resourceType) ?: return
        log.info("Provisioning DELETE {} {} to target", event.resourceType, event.resourceId)
        client.delete(handler.endpoint, event.resourceId)
    }

    private fun findHandler(resourceType: String): ResourceHandler<*>? {
        val handler = handlers.firstOrNull { it.resourceType.simpleName == resourceType }
        if (handler == null) {
            log.warn("No handler found for resource type: {}", resourceType)
        }
        return handler
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : com.marcosbarbero.scim2.core.domain.model.resource.ScimResource> ResourceHandler<*>.get(
        id: String,
        context: ScimRequestContext,
    ): T = (this as ResourceHandler<T>).get(id, context)

    @Suppress("UNCHECKED_CAST")
    private fun <T : com.marcosbarbero.scim2.core.domain.model.resource.ScimResource> ScimClient.create(
        endpoint: String,
        resource: T,
        type: kotlin.reflect.KClass<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        create(endpoint, resource, type as kotlin.reflect.KClass<T>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : com.marcosbarbero.scim2.core.domain.model.resource.ScimResource> ScimClient.replace(
        endpoint: String,
        id: String,
        resource: T,
        type: kotlin.reflect.KClass<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        replace(endpoint, id, resource, type as kotlin.reflect.KClass<T>)
    }

    private fun provisioningContext(): ScimRequestContext = ScimRequestContext(
        principalId = "scim-provisioning",
        roles = setOf("admin"),
    )
}
