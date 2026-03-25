package com.marcosbarbero.scim2.core.domain.model.patch

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcosbarbero.scim2.core.domain.ScimUrns

data class PatchRequest(
    val schemas: List<String> = listOf(ScimUrns.PATCH_OP),
    @JsonProperty("Operations")
    val operations: List<PatchOperation> = emptyList()
)
