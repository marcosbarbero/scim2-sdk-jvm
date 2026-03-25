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
package com.marcosbarbero.scim2.server.adapter.discovery

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.Schema
import com.marcosbarbero.scim2.core.domain.model.schema.ServiceProviderConfig
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.port.ResourceHandler

class DiscoveryService(
    private val handlers: List<ResourceHandler<*>>,
    private val schemaRegistry: SchemaRegistry,
    private val config: ScimServerConfig
) {

    fun getServiceProviderConfig(): ServiceProviderConfig =
        ServiceProviderConfig(
            patch = ServiceProviderConfig.SupportedConfig(supported = config.patchEnabled),
            bulk = ServiceProviderConfig.BulkConfig(
                supported = config.bulkEnabled,
                maxOperations = config.bulkMaxOperations,
                maxPayloadSize = config.bulkMaxPayloadSize
            ),
            filter = ServiceProviderConfig.FilterConfig(
                supported = config.filterEnabled,
                maxResults = config.filterMaxResults
            ),
            changePassword = ServiceProviderConfig.SupportedConfig(supported = config.changePasswordEnabled),
            sort = ServiceProviderConfig.SupportedConfig(supported = config.sortEnabled),
            etag = ServiceProviderConfig.SupportedConfig(supported = config.etagEnabled)
        )

    fun getSchemas(): ListResponse<Schema> {
        val schemas = schemaRegistry.getAllSchemas()
        return ListResponse(
            totalResults = schemas.size,
            itemsPerPage = schemas.size,
            startIndex = 1,
            resources = schemas
        )
    }

    fun getSchema(id: String): Schema =
        schemaRegistry.getSchema(id)
            ?: throw ResourceNotFoundException("Schema not found: $id")

    fun getResourceTypes(): ListResponse<ResourceType> {
        val types = schemaRegistry.getAllResourceTypes()
        return ListResponse(
            totalResults = types.size,
            itemsPerPage = types.size,
            startIndex = 1,
            resources = types
        )
    }

    fun getResourceType(name: String): ResourceType =
        schemaRegistry.getResourceType(name)
            ?: throw ResourceNotFoundException("ResourceType not found: $name")
}
