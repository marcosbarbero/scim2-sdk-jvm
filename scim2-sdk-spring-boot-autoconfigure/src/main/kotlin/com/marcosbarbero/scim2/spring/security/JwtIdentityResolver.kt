/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marcosbarbero.scim2.spring.security

import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.port.IdentityResolver
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import com.marcosbarbero.scim2.spring.autoconfigure.ScimProperties.ClaimMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Base [IdentityResolver] that extracts identity from a JWT in Spring Security's [SecurityContextHolder].
 * Subclasses customize claim extraction for specific IdPs.
 * All claim names are configurable via [ClaimMapping].
 */
open class JwtIdentityResolver(
    protected val claims: ClaimMapping = ClaimMapping()
) : IdentityResolver {

    override fun resolve(request: ScimHttpRequest): ScimRequestContext {
        val authentication = SecurityContextHolder.getContext().authentication
        val jwt = (authentication as? JwtAuthenticationToken)?.token

        return jwt?.let {
            ScimRequestContext(
                principalId = extractPrincipal(it),
                roles = extractRoles(it),
                attributes = extractAttributes(it)
            )
        } ?: ScimRequestContext()
    }

    protected open fun extractPrincipal(jwt: Jwt): String =
        jwt.getClaimAsString(claims.subject) ?: jwt.subject ?: "unknown"

    protected open fun extractRoles(jwt: Jwt): Set<String> {
        val roles = jwt.getClaimAsStringList(claims.roles)
        return roles?.toSet() ?: emptySet()
    }

    protected open fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        jwt.getClaimAsString(claims.email)?.let { attrs["email"] = it }
        jwt.getClaimAsString(claims.name)?.let { attrs["name"] = it }
        jwt.issuer?.let { attrs["issuer"] = it.toString() }
        return attrs
    }
}
