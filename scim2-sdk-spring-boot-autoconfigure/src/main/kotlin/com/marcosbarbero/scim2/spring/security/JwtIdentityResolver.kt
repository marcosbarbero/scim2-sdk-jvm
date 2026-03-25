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
        val jwt = when (authentication) {
            is JwtAuthenticationToken -> authentication.token
            else -> null
        }

        return if (jwt != null) {
            ScimRequestContext(
                principalId = extractPrincipal(jwt),
                roles = extractRoles(jwt),
                attributes = extractAttributes(jwt)
            )
        } else {
            ScimRequestContext()
        }
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
