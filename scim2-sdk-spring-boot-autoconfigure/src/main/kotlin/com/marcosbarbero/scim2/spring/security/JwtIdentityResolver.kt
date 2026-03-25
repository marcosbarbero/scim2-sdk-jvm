package com.marcosbarbero.scim2.spring.security

import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.port.IdentityResolver
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Base [IdentityResolver] that extracts identity from a JWT in Spring Security's [SecurityContextHolder].
 * Subclasses customize claim extraction for specific IdPs.
 */
open class JwtIdentityResolver(
    private val subjectClaim: String = "sub",
    private val rolesClaim: String = "roles",
    private val emailClaim: String = "email"
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
        jwt.getClaimAsString(subjectClaim) ?: jwt.subject ?: "unknown"

    protected open fun extractRoles(jwt: Jwt): Set<String> {
        val roles = jwt.getClaimAsStringList(rolesClaim)
        return roles?.toSet() ?: emptySet()
    }

    protected open fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        jwt.getClaimAsString(emailClaim)?.let { attrs["email"] = it }
        jwt.getClaimAsString("name")?.let { attrs["name"] = it }
        jwt.issuer?.let { attrs["issuer"] = it.toString() }
        return attrs
    }
}
