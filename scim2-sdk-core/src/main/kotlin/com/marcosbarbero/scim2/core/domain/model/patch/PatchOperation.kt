package com.marcosbarbero.scim2.core.domain.model.patch

data class PatchOperation(
    val op: PatchOp,
    val path: String? = null,
    val value: Any? = null
)
