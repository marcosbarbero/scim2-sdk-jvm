package com.marcosbarbero.scim2.spring.security

import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Keycloak.
 * Extracts roles from `realm_access.roles` and `resource_access.{client}.roles` claims.
 */
class KeycloakIdentityResolver(
    private val clientId: String? = null
) : JwtIdentityResolver(subjectClaim = "sub", rolesClaim = "realm_access", emailClaim = "email") {

    @Suppress("UNCHECKED_CAST")
    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        // Realm roles
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        (realmAccess?.get("roles") as? List<String>)?.let { roles.addAll(it) }
        // Client roles
        if (clientId != null) {
            val resourceAccess = jwt.getClaim<Map<String, Any>>("resource_access")
            val clientAccess = resourceAccess?.get(clientId) as? Map<String, Any>
            (clientAccess?.get("roles") as? List<String>)?.let { roles.addAll(it) }
        }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString("preferred_username")?.let { attrs["preferred_username"] = it }
        jwt.getClaimAsString("given_name")?.let { attrs["given_name"] = it }
        jwt.getClaimAsString("family_name")?.let { attrs["family_name"] = it }
        return attrs
    }
}
