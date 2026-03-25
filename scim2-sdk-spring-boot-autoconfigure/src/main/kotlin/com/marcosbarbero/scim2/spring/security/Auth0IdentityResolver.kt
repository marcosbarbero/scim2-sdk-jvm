package com.marcosbarbero.scim2.spring.security

import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Auth0.
 * Extracts roles from custom namespace claims and permissions.
 */
class Auth0IdentityResolver(
    private val namespace: String = "https://your-app.auth0.com"
) : JwtIdentityResolver(subjectClaim = "sub", emailClaim = "email") {

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList("${namespace}/roles")?.let { roles.addAll(it) }
        jwt.getClaimAsStringList("permissions")?.let { roles.addAll(it) }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString("nickname")?.let { attrs["nickname"] = it }
        jwt.getClaimAsString("picture")?.let { attrs["picture"] = it }
        return attrs
    }
}
