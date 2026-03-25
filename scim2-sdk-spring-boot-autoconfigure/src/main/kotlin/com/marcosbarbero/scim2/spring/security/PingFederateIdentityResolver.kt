package com.marcosbarbero.scim2.spring.security

import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for PingFederate.
 * Extracts roles from `memberOf` or `groups` claims.
 */
class PingFederateIdentityResolver : JwtIdentityResolver(subjectClaim = "sub", emailClaim = "email") {

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList("memberOf")?.let { roles.addAll(it) }
        jwt.getClaimAsStringList("groups")?.let { roles.addAll(it) }
        return roles
    }
}
