package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest

interface ResourceHandler<T : ScimResource> {

    val resourceType: Class<T>

    val endpoint: String

    fun get(id: String, context: ScimRequestContext): T

    fun create(resource: T, context: ScimRequestContext): T

    fun replace(id: String, resource: T, version: String?, context: ScimRequestContext): T

    fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): T

    fun delete(id: String, version: String?, context: ScimRequestContext)

    fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T>
}
