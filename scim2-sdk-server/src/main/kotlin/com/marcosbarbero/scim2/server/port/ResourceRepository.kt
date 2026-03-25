package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest

interface ResourceRepository<T : ScimResource> {

    fun findById(id: String): T?

    fun create(resource: T): T

    fun replace(id: String, resource: T, version: String?): T

    fun delete(id: String, version: String?)

    fun search(request: SearchRequest): ListResponse<T>
}
