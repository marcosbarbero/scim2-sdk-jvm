package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.domain.vo.ResourceId

interface ResourceHandler<T : ScimResource> {

    val resourceType: Class<T>

    val endpoint: String

    fun get(id: ResourceId, context: ScimRequestContext): T

    fun create(resource: T, context: ScimRequestContext): T

    fun replace(id: ResourceId, resource: T, version: ETag?, context: ScimRequestContext): T

    fun patch(id: ResourceId, request: PatchRequest, version: ETag?, context: ScimRequestContext): T

    fun delete(id: ResourceId, version: ETag?, context: ScimRequestContext)

    fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T>
}
