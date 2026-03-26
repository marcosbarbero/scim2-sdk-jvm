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
 * [JwtIdentityResolver] for Azure AD (Microsoft Entra ID).
 * Extracts roles from `roles` claim and app roles from `wids` claim.
 * Uses `oid` (Object ID) as the principal identifier.
 * Common claim names are configurable via [ClaimMapping]; Azure AD-specific claim names
 * default to Azure AD conventions and can be overridden via constructor parameters.
 */
class AzureAdIdentityResolver(
    claims: ClaimMapping = ClaimMapping(),
    private val objectIdClaim: String = "oid",
    private val directoryRolesClaim: String = "wids",
    private val preferredUsernameClaim: String = "preferred_username",
    private val tenantIdClaim: String = "tid",
    private val appIdClaim: String = "appid",
) : JwtIdentityResolver(claims) {

    override fun extractPrincipal(jwt: Jwt): String = jwt.getClaimAsString(objectIdClaim) ?: jwt.getClaimAsString(claims.subject) ?: "unknown"

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList(claims.roles)?.let { roles.addAll(it) }
        jwt.getClaimAsStringList(directoryRolesClaim)?.let { roles.addAll(it) }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString(preferredUsernameClaim)?.let { attrs["preferred_username"] = it }
        jwt.getClaimAsString(tenantIdClaim)?.let { attrs["tenant_id"] = it }
        jwt.getClaimAsString(appIdClaim)?.let { attrs["app_id"] = it }
        return attrs
    }
}
