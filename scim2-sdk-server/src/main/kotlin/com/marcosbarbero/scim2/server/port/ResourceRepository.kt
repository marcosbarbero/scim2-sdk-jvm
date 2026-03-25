package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.domain.vo.ResourceId

interface ResourceRepository<T : ScimResource> {

    fun findById(id: ResourceId): T?

    fun create(resource: T): T

    fun replace(id: ResourceId, resource: T, version: ETag?): T

    fun delete(id: ResourceId, version: ETag?)

    fun search(request: SearchRequest): ListResponse<T>
}
