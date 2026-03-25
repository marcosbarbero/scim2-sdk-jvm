package com.marcosbarbero.scim2.core.domain.model.error

data class ScimError(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
    val status: String,
    val scimType: String? = null,
    val detail: String? = null
)
