package com.marcosbarbero.scim2.core.domain.model.bulk

import tools.jackson.databind.JsonNode

data class BulkOperation(
    val method: String,
    val path: String? = null,
    val bulkId: String? = null,
    val data: JsonNode? = null
)
