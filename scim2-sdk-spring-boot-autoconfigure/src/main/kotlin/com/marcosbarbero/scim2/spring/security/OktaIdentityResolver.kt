package com.marcosbarbero.scim2.spring.security

import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Okta.
 * Extracts roles from the `groups` or `scp` (scopes) claims.
 */
class OktaIdentityResolver : JwtIdentityResolver(subjectClaim = "sub", emailClaim = "email") {

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList("groups")?.let { roles.addAll(it) }
        jwt.getClaimAsStringList("scp")?.let { roles.addAll(it) }
        return roles
    }
}
