package com.marcosbarbero.scim2.core.domain.model.bulk

import com.fasterxml.jackson.annotation.JsonProperty

data class BulkRequest(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:BulkRequest"),
    val failOnErrors: Int? = null,
    @JsonProperty("Operations")
    val operations: List<BulkOperation> = emptyList()
)
