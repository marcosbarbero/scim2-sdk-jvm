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
package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.core.observability.ScimTracer
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.interceptor.ScimInterceptor
import com.marcosbarbero.scim2.server.port.AuthorizationEvaluator
import com.marcosbarbero.scim2.server.port.BulkHandler
import com.marcosbarbero.scim2.server.port.IdentityResolver
import com.marcosbarbero.scim2.server.port.MeHandler
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.spring.handler.DefaultResourceHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.ObjectMapper

@AutoConfiguration(after = [ScimJacksonAutoConfiguration::class])
@ConditionalOnClass(ScimEndpointDispatcher::class)
@EnableConfigurationProperties(ScimProperties::class)
class ScimServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun scimServerConfig(properties: ScimProperties): ScimServerConfig = ScimServerConfig(
        basePath = properties.basePath,
        bulkEnabled = properties.bulk.enabled,
        bulkMaxOperations = properties.bulk.maxOperations,
        bulkMaxPayloadSize = properties.bulk.maxPayloadSize,
        filterEnabled = properties.filter.enabled,
        filterMaxResults = properties.filter.maxResults,
        sortEnabled = properties.sort.enabled,
        etagEnabled = properties.etag.enabled,
        changePasswordEnabled = properties.changePassword.enabled,
        patchEnabled = properties.patch.enabled,
        defaultPageSize = properties.pagination.defaultPageSize,
        maxPageSize = properties.pagination.maxPageSize,
    )

    @Bean
    @ConditionalOnMissingBean
    fun schemaRegistry(handlers: ObjectProvider<ResourceHandler<*>>): SchemaRegistry {
        val registry = SchemaRegistry()
        handlers.orderedStream().forEach { handler ->
            registry.register(handler.resourceType.kotlin)
        }
        return registry
    }

    @Bean
    @ConditionalOnMissingBean
    fun discoveryService(
        handlers: ObjectProvider<ResourceHandler<*>>,
        schemaRegistry: SchemaRegistry,
        config: ScimServerConfig,
    ): DiscoveryService = DiscoveryService(
        handlers = handlers.orderedStream().toList(),
        schemaRegistry = schemaRegistry,
        config = config,
    )

    @Bean
    @ConditionalOnMissingBean
    fun scimEndpointDispatcher(
        handlers: ObjectProvider<ResourceHandler<*>>,
        discoveryService: DiscoveryService,
        config: ScimServerConfig,
        serializer: ScimSerializer,
        interceptors: ObjectProvider<ScimInterceptor>,
        bulkHandler: ObjectProvider<BulkHandler>,
        meHandler: ObjectProvider<MeHandler<*>>,
        identityResolver: ObjectProvider<IdentityResolver>,
        authorizationEvaluator: ObjectProvider<AuthorizationEvaluator>,
        eventPublisher: ObjectProvider<ScimEventPublisher>,
        metrics: ObjectProvider<ScimMetrics>,
        tracer: ObjectProvider<ScimTracer>,
    ): ScimEndpointDispatcher = ScimEndpointDispatcher(
        handlers = handlers.orderedStream().toList(),
        bulkHandler = bulkHandler.ifAvailable,
        meHandler = meHandler.ifAvailable,
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
        interceptors = interceptors.orderedStream().toList(),
        identityResolver = identityResolver.ifAvailable,
        authorizationEvaluator = authorizationEvaluator.ifAvailable,
        eventPublisher = eventPublisher.ifAvailable ?: com.marcosbarbero.scim2.core.event.NoOpEventPublisher,
        metrics = metrics.ifAvailable ?: com.marcosbarbero.scim2.core.observability.NoOpScimMetrics,
        tracer = tracer.ifAvailable ?: com.marcosbarbero.scim2.core.observability.NoOpScimTracer,
    )

    @Bean
    @ConditionalOnMissingBean(name = ["scimUserHandler"])
    fun scimUserHandler(repository: ObjectProvider<ResourceRepository<User>>, objectMapper: ObjectProvider<ObjectMapper>): ResourceHandler<User>? {
        val repo = repository.ifAvailable ?: return null
        return DefaultResourceHandler(User::class.java, "/Users", repo, objectMapper.ifAvailable)
    }

    @Bean
    @ConditionalOnMissingBean(name = ["scimGroupHandler"])
    fun scimGroupHandler(repository: ObjectProvider<ResourceRepository<Group>>, objectMapper: ObjectProvider<ObjectMapper>): ResourceHandler<Group>? {
        val repo = repository.ifAvailable ?: return null
        return DefaultResourceHandler(Group::class.java, "/Groups", repo, objectMapper.ifAvailable)
    }
}
