package com.marcosbarbero.scim2.core.domain.model.patch

import tools.jackson.databind.JsonNode

data class PatchOperation(
    val op: PatchOp,
    val path: String? = null,
    val value: JsonNode? = null
)
