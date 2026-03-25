package com.marcosbarbero.scim2.core.domain.model.patch

import com.fasterxml.jackson.annotation.JsonProperty

data class PatchRequest(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp"),
    @JsonProperty("Operations")
    val operations: List<PatchOperation> = emptyList()
)
