package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource

interface MeHandler<T : ScimResource> {

    fun getMe(context: ScimRequestContext): T

    fun replaceMe(context: ScimRequestContext, resource: T, version: String?): T

    fun patchMe(context: ScimRequestContext, request: PatchRequest, version: String?): T

    fun deleteMe(context: ScimRequestContext, version: String?)
}
