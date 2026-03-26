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
package com.marcosbarbero.scim2.spring.handler

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.patch.PatchEngine
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import tools.jackson.databind.ObjectMapper

class DefaultResourceHandler<T : ScimResource>(
    override val resourceType: Class<T>,
    override val endpoint: String,
    private val repository: ResourceRepository<T>,
    private val objectMapper: ObjectMapper? = null,
) : ResourceHandler<T> {

    override fun get(id: String, context: ScimRequestContext): T = repository.findById(id)
        ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")

    override fun create(resource: T, context: ScimRequestContext): T = repository.create(resource)

    override fun replace(id: String, resource: T, version: String?, context: ScimRequestContext): T = repository.replace(id, resource, version)

    override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): T {
        val existing = repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        val mapper = objectMapper
            ?: throw UnsupportedOperationException("PATCH not supported: no ObjectMapper configured")
        val patched = PatchEngine(mapper).apply(existing, request)
        return repository.replace(id, patched, version)
    }

    override fun delete(id: String, version: String?, context: ScimRequestContext) {
        repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        repository.delete(id, version)
    }

    override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T> = repository.search(request)
}
