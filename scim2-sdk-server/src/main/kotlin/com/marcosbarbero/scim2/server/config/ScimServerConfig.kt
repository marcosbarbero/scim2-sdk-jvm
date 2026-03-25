package com.marcosbarbero.scim2.server.config

data class ScimServerConfig(
    val basePath: String = "/scim/v2",
    val bulkEnabled: Boolean = true,
    val bulkMaxOperations: Int = 1000,
    val bulkMaxPayloadSize: Long = 1_048_576,
    val filterEnabled: Boolean = true,
    val filterMaxResults: Int = 200,
    val sortEnabled: Boolean = false,
    val etagEnabled: Boolean = true,
    val changePasswordEnabled: Boolean = false,
    val patchEnabled: Boolean = true,
    val defaultPageSize: Int = 100,
    val maxPageSize: Int = 1000
)
