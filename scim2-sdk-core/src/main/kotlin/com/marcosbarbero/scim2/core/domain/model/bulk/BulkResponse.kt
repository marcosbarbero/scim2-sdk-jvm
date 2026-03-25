package com.marcosbarbero.scim2.core.domain.model.bulk

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcosbarbero.scim2.core.domain.ScimUrns

data class BulkResponse(
    val schemas: List<String> = listOf(ScimUrns.BULK_RESPONSE),
    @JsonProperty("Operations")
    val operations: List<BulkOperationResponse> = emptyList()
)
