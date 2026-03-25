package com.marcosbarbero.scim2.core.domain.model.bulk

data class BulkOperationResponse(
    val method: String,
    val bulkId: String? = null,
    val status: String,
    val location: String? = null,
    val response: Any? = null
)
