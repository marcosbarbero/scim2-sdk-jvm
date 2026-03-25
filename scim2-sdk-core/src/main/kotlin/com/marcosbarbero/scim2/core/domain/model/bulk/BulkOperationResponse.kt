package com.marcosbarbero.scim2.core.domain.model.bulk

import tools.jackson.databind.JsonNode

data class BulkOperationResponse(
    val method: String,
    val bulkId: String? = null,
    val status: String,
    val location: String? = null,
    val response: JsonNode? = null
)
