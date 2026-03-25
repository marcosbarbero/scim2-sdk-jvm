package com.marcosbarbero.scim2.client.api

data class ScimResponse<T>(
    val value: T,
    val statusCode: Int,
    val headers: Map<String, List<String>> = emptyMap(),
    val etag: String? = null,
    val location: String? = null
)
