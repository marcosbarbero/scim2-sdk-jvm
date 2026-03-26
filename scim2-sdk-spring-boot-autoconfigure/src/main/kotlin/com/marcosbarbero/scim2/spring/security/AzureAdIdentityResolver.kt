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
 * All claim names are configurable via [ClaimMapping].
 */
class AzureAdIdentityResolver(
    claims: ClaimMapping = ClaimMapping(),
) : JwtIdentityResolver(claims) {

    override fun extractPrincipal(jwt: Jwt): String = jwt.getClaimAsString(claims.objectId) ?: jwt.getClaimAsString(claims.subject) ?: "unknown"

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList(claims.roles)?.let { roles.addAll(it) }
        jwt.getClaimAsStringList(claims.directoryRoles)?.let { roles.addAll(it) }
        return roles
    }

    override fun extractAttributes(jwt: Jwt): Map<String, String> {
        val attrs = super.extractAttributes(jwt).toMutableMap()
        jwt.getClaimAsString(claims.preferredUsername)?.let { attrs["preferred_username"] = it }
        jwt.getClaimAsString(claims.tenantId)?.let { attrs["tenant_id"] = it }
        jwt.getClaimAsString(claims.appId)?.let { attrs["app_id"] = it }
        return attrs
    }
}
