package com.marcosbarbero.scim2.spring.security

import com.marcosbarbero.scim2.spring.autoconfigure.ScimProperties.ClaimMapping
import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Okta.
 * Extracts roles from the `groups` or `scp` (scopes) claims.
 * All claim names are configurable via [ClaimMapping].
 */
class OktaIdentityResolver(
    claims: ClaimMapping = ClaimMapping()
) : JwtIdentityResolver(claims) {

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList(claims.groups)?.let { roles.addAll(it) }
        jwt.getClaimAsStringList(claims.scopes)?.let { roles.addAll(it) }
        return roles
    }
}
