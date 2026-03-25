package com.marcosbarbero.scim2.core.domain.model.error

import com.marcosbarbero.scim2.core.domain.ScimUrns

data class ScimError(
    val schemas: List<String> = listOf(ScimUrns.ERROR),
    val status: String,
    val scimType: String? = null,
    val detail: String? = null
)
