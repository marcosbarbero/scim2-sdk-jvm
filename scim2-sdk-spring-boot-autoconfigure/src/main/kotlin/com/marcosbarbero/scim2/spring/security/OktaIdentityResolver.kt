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
 * [JwtIdentityResolver] for Okta.
 * Extracts roles from the `groups` or `scp` (scopes) claims.
 * Common claim names are configurable via [ClaimMapping]; the Okta-specific `scp` claim
 * defaults to Okta conventions and can be overridden via the constructor parameter.
 */
class OktaIdentityResolver(
    claims: ClaimMapping = ClaimMapping(),
    private val scopesClaim: String = "scp",
) : JwtIdentityResolver(claims) {

    override fun extractRoles(jwt: Jwt): Set<String> {
        val roles = mutableSetOf<String>()
        jwt.getClaimAsStringList(claims.groups)?.let { roles.addAll(it) }
        jwt.getClaimAsStringList(scopesClaim)?.let { roles.addAll(it) }
        return roles
    }
}
