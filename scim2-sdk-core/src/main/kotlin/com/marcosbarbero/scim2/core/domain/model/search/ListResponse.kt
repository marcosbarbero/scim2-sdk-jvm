package com.marcosbarbero.scim2.core.domain.model.search

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcosbarbero.scim2.core.domain.ScimUrns

data class ListResponse<T>(
    val schemas: List<String> = listOf(ScimUrns.LIST_RESPONSE),
    val totalResults: Int,
    val itemsPerPage: Int? = null,
    val startIndex: Int? = null,
    @JsonProperty("Resources")
    val resources: List<T> = emptyList()
)
