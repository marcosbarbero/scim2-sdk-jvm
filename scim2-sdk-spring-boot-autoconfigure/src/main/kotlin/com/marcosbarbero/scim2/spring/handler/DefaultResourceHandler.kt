package com.marcosbarbero.scim2.spring.handler

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.server.port.ScimRequestContext

class DefaultResourceHandler<T : ScimResource>(
    override val resourceType: Class<T>,
    override val endpoint: String,
    private val repository: ResourceRepository<T>
) : ResourceHandler<T> {

    override fun get(id: ResourceId, context: ScimRequestContext): T =
        repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: ${id.value}")

    override fun create(resource: T, context: ScimRequestContext): T =
        repository.create(resource)

    override fun replace(id: ResourceId, resource: T, version: ETag?, context: ScimRequestContext): T =
        repository.replace(id, resource, version)

    override fun patch(id: ResourceId, request: PatchRequest, version: ETag?, context: ScimRequestContext): T {
        val existing = repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: ${id.value}")
        // Apply patch operations and replace
        // For now, delegate to replace with the existing resource
        // Real patch logic would apply each operation to the existing resource
        return repository.replace(id, existing, version)
    }

    override fun delete(id: ResourceId, version: ETag?, context: ScimRequestContext) {
        repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: ${id.value}")
        repository.delete(id, version)
    }

    override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T> =
        repository.search(request)
}
