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

import com.marcosbarbero.scim2.spring.autoconfigure.ScimProperties.ClaimMapping
import org.springframework.security.oauth2.jwt.Jwt

/**
 * [JwtIdentityResolver] for Keycloak.
 * Extracts roles from `realm_access.roles` and `resource_access.{client}.roles` claims.
 * All claim names are configurable via [ClaimMapping].
 */
class KeycloakIdentityResolver(
    private val clientId: String? = null,
    claims: ClaimMapping = ClaimMapping(),
) : JwtIdentityResolver(claims) {

    @Suppress("UNCHECKED_CAST")
    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        // Realm roles
        val realmAccess = jwt.getClaim<Map<String, Any>>(claims.realmAccess)
        (realmAccess?.get("roles") as? List<String>)?.let { roles.addAll(it) }
        // Client roles
        clientId?.let { cid ->
            val resourceAccess = jwt.getClaim<Map<String, Any>>(claims.resourceAccess)
            val clientAccess = resourceAccess?.get(cid) as? Map<String, Any>
            (clientAccess?.get("roles") as? List<String>)?.let { roles.addAll(it) }
        }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString(claims.preferredUsername)?.let { attrs["preferred_username"] = it }
        jwt.getClaimAsString(claims.givenName)?.let { attrs["given_name"] = it }
        jwt.getClaimAsString(claims.familyName)?.let { attrs["family_name"] = it }
        return attrs
    }
}
