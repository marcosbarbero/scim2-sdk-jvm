package com.marcosbarbero.scim2.core.domain.model.patch

import com.fasterxml.jackson.databind.JsonNode

data class PatchOperation(
    val op: PatchOp,
    val path: String? = null,
    val value: JsonNode? = null
)
