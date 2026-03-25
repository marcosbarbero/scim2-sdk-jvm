package com.marcosbarbero.scim2.test.handler

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository

class InMemoryResourceHandler<T : ScimResource>(
    override val resourceType: Class<T>,
    override val endpoint: String,
    val repository: InMemoryResourceRepository<T>,
    private val patchApplier: ((T, PatchRequest) -> T)? = null
) : ResourceHandler<T> {

    override fun get(id: ResourceId, context: ScimRequestContext): T {
        return repository.findById(id)
            ?: throw ResourceNotFoundException("Resource not found: ${id.value}")
    }

    override fun create(resource: T, context: ScimRequestContext): T {
        return repository.create(resource)
    }

    override fun replace(id: ResourceId, resource: T, version: ETag?, context: ScimRequestContext): T {
        return repository.replace(id, resource, version)
    }

    override fun patch(id: ResourceId, request: PatchRequest, version: ETag?, context: ScimRequestContext): T {
        val existing = repository.findById(id)
            ?: throw ResourceNotFoundException("Resource not found: ${id.value}")

        val patched = if (patchApplier != null) {
            patchApplier.invoke(existing, request)
        } else {
            // Default no-op: return existing resource unchanged for operations we cannot apply generically
            existing
        }

        return repository.replace(id, patched, version)
    }

    override fun delete(id: ResourceId, version: ETag?, context: ScimRequestContext) {
        repository.delete(id, version)
    }

    override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T> {
        return repository.search(request)
    }
}
