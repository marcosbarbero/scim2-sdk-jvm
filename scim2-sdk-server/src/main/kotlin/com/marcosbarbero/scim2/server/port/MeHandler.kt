package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.vo.ETag

interface MeHandler<T : ScimResource> {

    fun getMe(context: ScimRequestContext): T

    fun replaceMe(context: ScimRequestContext, resource: T, version: ETag?): T

    fun patchMe(context: ScimRequestContext, request: PatchRequest, version: ETag?): T

    fun deleteMe(context: ScimRequestContext, version: ETag?)
}
