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
package com.marcosbarbero.scim2.spring.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scim")
data class ScimProperties(
    val basePath: String = "/scim/v2",
    val bulk: BulkProperties = BulkProperties(),
    val filter: FilterProperties = FilterProperties(),
    val sort: SortProperties = SortProperties(),
    val etag: EtagProperties = EtagProperties(),
    val changePassword: ChangePasswordProperties = ChangePasswordProperties(),
    val patch: PatchProperties = PatchProperties(),
    val pagination: PaginationProperties = PaginationProperties(),
    val client: ClientProperties = ClientProperties(),
    val persistence: PersistenceProperties = PersistenceProperties(),
    val idp: IdpProperties = IdpProperties(),
    val provisioning: ProvisioningProperties = ProvisioningProperties(),
) {
    data class IdpProperties(
        val provider: String? = null,
        val clientId: String? = null,
        val namespace: String? = null,
        val claims: ClaimMapping = ClaimMapping(),
    )

    data class ClaimMapping(
        // Common JWT claims
        val subject: String = "sub",
        val email: String = "email",
        val name: String = "name",
        val roles: String = "roles",
        val groups: String = "groups",
        // Additional custom claims as a map
        val custom: Map<String, String> = emptyMap(),
    )

    data class BulkProperties(
        val enabled: Boolean = true,
        val maxOperations: Int = 1000,
        val maxPayloadSize: Long = 1_048_576,
    )

    data class FilterProperties(
        val enabled: Boolean = true,
        val maxResults: Int = 200,
    )

    data class SortProperties(
        val enabled: Boolean = false,
    )

    data class EtagProperties(
        val enabled: Boolean = true,
    )

    data class ChangePasswordProperties(
        val enabled: Boolean = false,
    )

    data class PatchProperties(
        val enabled: Boolean = true,
    )

    data class PaginationProperties(
        val defaultPageSize: Int = 100,
        val maxPageSize: Int = 1000,
    )

    data class ClientProperties(
        val baseUrl: String? = null,
        val connectTimeout: java.time.Duration = java.time.Duration.ofSeconds(10),
        val readTimeout: java.time.Duration = java.time.Duration.ofSeconds(30),
    )

    data class ProvisioningProperties(
        val enabled: Boolean = false,
        val targetUrl: String? = null,
        val bearerToken: String? = null,
    )

    data class PersistenceProperties(
        val enabled: Boolean = false,
        val tableName: String = "scim_resources",
        val schemaName: String? = null,
        val autoMigrate: Boolean = false,
    )
}
