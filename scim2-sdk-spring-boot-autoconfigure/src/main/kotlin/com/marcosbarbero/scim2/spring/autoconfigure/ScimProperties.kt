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
    val client: ClientProperties = ClientProperties()
) {
    data class BulkProperties(
        val enabled: Boolean = true,
        val maxOperations: Int = 1000,
        val maxPayloadSize: Long = 1_048_576
    )

    data class FilterProperties(
        val enabled: Boolean = true,
        val maxResults: Int = 200
    )

    data class SortProperties(
        val enabled: Boolean = false
    )

    data class EtagProperties(
        val enabled: Boolean = true
    )

    data class ChangePasswordProperties(
        val enabled: Boolean = false
    )

    data class PatchProperties(
        val enabled: Boolean = true
    )

    data class PaginationProperties(
        val defaultPageSize: Int = 100,
        val maxPageSize: Int = 1000
    )

    data class ClientProperties(
        val baseUrl: String? = null,
        val connectTimeout: java.time.Duration = java.time.Duration.ofSeconds(10),
        val readTimeout: java.time.Duration = java.time.Duration.ofSeconds(30)
    )
}
