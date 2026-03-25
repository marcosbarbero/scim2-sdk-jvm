package com.marcosbarbero.scim2.core.domain.model.bulk

import com.fasterxml.jackson.annotation.JsonProperty

data class BulkResponse(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:BulkResponse"),
    @JsonProperty("Operations")
    val operations: List<BulkOperationResponse> = emptyList()
)
