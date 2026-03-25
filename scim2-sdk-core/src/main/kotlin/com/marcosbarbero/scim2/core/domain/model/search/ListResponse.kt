package com.marcosbarbero.scim2.core.domain.model.search

import com.fasterxml.jackson.annotation.JsonProperty

data class ListResponse<T>(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
    val totalResults: Int,
    val itemsPerPage: Int? = null,
    val startIndex: Int? = null,
    @JsonProperty("Resources")
    val resources: List<T> = emptyList()
)
