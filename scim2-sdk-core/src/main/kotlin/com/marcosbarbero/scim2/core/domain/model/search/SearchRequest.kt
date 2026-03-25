package com.marcosbarbero.scim2.core.domain.model.search

import com.marcosbarbero.scim2.core.domain.ScimUrns

data class SearchRequest(
    val schemas: List<String> = listOf(ScimUrns.SEARCH_REQUEST),
    val filter: String? = null,
    val sortBy: String? = null,
    val sortOrder: SortOrder? = null,
    val startIndex: Int? = null,
    val count: Int? = null,
    val attributes: List<String>? = null,
    val excludedAttributes: List<String>? = null
)
