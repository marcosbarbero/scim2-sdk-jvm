package com.marcosbarbero.scim2.core.domain.model.schema

data class ServiceProviderConfig(
    val documentationUri: String? = null,
    val patch: SupportedConfig = SupportedConfig(),
    val bulk: BulkConfig = BulkConfig(),
    val filter: FilterConfig = FilterConfig(),
    val changePassword: SupportedConfig = SupportedConfig(),
    val sort: SupportedConfig = SupportedConfig(),
    val etag: SupportedConfig = SupportedConfig(),
    val authenticationSchemes: List<AuthenticationScheme> = emptyList()
) {
    data class SupportedConfig(val supported: Boolean = false)

    data class BulkConfig(
        val supported: Boolean = false,
        val maxOperations: Int = 0,
        val maxPayloadSize: Long = 0
    )

    data class FilterConfig(
        val supported: Boolean = false,
        val maxResults: Int = 0
    )

    data class AuthenticationScheme(
        val type: String,
        val name: String,
        val description: String,
        val specUri: String? = null,
        val documentationUri: String? = null
    )
}
