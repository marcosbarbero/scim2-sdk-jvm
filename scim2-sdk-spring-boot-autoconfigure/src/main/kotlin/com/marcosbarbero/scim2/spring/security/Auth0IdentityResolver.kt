package com.marcosbarbero.scim2.spring.security

import com.marcosbarbero.scim2.spring.autoconfigure.ScimProperties.ClaimMapping
import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Auth0.
 * Extracts roles from custom namespace claims and permissions.
 * All claim names are configurable via [ClaimMapping].
 */
class Auth0IdentityResolver(
    private val namespace: String = "https://your-app.auth0.com",
    claims: ClaimMapping = ClaimMapping()
) : JwtIdentityResolver(claims) {

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList("${namespace}/${claims.roles}")?.let { roles.addAll(it) }
        jwt.getClaimAsStringList(claims.permissions)?.let { roles.addAll(it) }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString(claims.nickname)?.let { attrs["nickname"] = it }
        jwt.getClaimAsString(claims.picture)?.let { attrs["picture"] = it }
        return attrs
    }
}
