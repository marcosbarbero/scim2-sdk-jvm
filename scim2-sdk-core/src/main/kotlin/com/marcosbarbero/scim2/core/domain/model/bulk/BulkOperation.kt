package com.marcosbarbero.scim2.core.domain.model.bulk

data class BulkOperation(
    val method: String,
    val path: String? = null,
    val bulkId: String? = null,
    val data: Any? = null
)
