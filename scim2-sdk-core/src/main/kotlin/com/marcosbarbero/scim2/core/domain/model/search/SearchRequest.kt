package com.marcosbarbero.scim2.core.domain.model.search

data class SearchRequest(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:SearchRequest"),
    val filter: String? = null,
    val sortBy: String? = null,
    val sortOrder: SortOrder? = null,
    val startIndex: Int? = null,
    val count: Int? = null,
    val attributes: List<String>? = null,
    val excludedAttributes: List<String>? = null
)
