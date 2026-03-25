package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.server.http.ScimHttpRequest

interface IdentityResolver {

    fun resolve(request: ScimHttpRequest): ScimRequestContext
}
