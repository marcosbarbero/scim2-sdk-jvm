package com.marcosbarbero.scim2.spring.security

import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Azure AD (Microsoft Entra ID).
 * Extracts roles from `roles` claim and app roles from `wids` claim.
 * Uses `oid` (Object ID) as the principal identifier.
 */
class AzureAdIdentityResolver : JwtIdentityResolver(subjectClaim = "oid", emailClaim = "email") {

    override fun extractPrincipal(jwt: Jwt): String =
        jwt.getClaimAsString("oid") ?: jwt.getClaimAsString("sub") ?: "unknown"

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList("roles")?.let { roles.addAll(it) }
        jwt.getClaimAsStringList("wids")?.let { roles.addAll(it) }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString("preferred_username")?.let { attrs["preferred_username"] = it }
        jwt.getClaimAsString("tid")?.let { attrs["tenant_id"] = it }
        jwt.getClaimAsString("appid")?.let { attrs["app_id"] = it }
        return attrs
    }
}
