package com.marcosbarbero.scim2.spring.handler

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.server.port.ScimRequestContext

class DefaultResourceHandler<T : ScimResource>(
    override val resourceType: Class<T>,
    override val endpoint: String,
    private val repository: ResourceRepository<T>
) : ResourceHandler<T> {

    override fun get(id: String, context: ScimRequestContext): T =
        repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")

    override fun create(resource: T, context: ScimRequestContext): T =
        repository.create(resource)

    override fun replace(id: String, resource: T, version: String?, context: ScimRequestContext): T =
        repository.replace(id, resource, version)

    override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): T {
        val existing = repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        // Apply patch operations and replace
        // For now, delegate to replace with the existing resource
        // Real patch logic would apply each operation to the existing resource
        return repository.replace(id, existing, version)
    }

    override fun delete(id: String, version: String?, context: ScimRequestContext) {
        repository.findById(id)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        repository.delete(id, version)
    }

    override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T> =
        repository.search(request)
}
