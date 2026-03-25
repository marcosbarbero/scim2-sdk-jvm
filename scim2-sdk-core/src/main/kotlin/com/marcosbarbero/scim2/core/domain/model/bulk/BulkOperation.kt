package com.marcosbarbero.scim2.core.domain.model.bulk

import com.fasterxml.jackson.databind.JsonNode

data class BulkOperation(
    val method: String,
    val path: String? = null,
    val bulkId: String? = null,
    val data: JsonNode? = null
)
