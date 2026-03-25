package com.marcosbarbero.scim2.core.domain.model.bulk

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcosbarbero.scim2.core.domain.ScimUrns

data class BulkRequest(
    val schemas: List<String> = listOf(ScimUrns.BULK_REQUEST),
    val failOnErrors: Int? = null,
    @JsonProperty("Operations")
    val operations: List<BulkOperation> = emptyList()
)
