package com.marcosbarbero.scim2.server.engine

import com.marcosbarbero.scim2.core.domain.model.search.ListResponse

class PaginationEngine {

    fun <T> paginate(
        items: List<T>,
        startIndex: Int,
        count: Int?,
        totalResults: Int = items.size
    ): ListResponse<T> {
        val effectiveStartIndex = maxOf(startIndex, 1)
        val zeroBasedStart = effectiveStartIndex - 1

        if (zeroBasedStart >= items.size) {
            return ListResponse(
                totalResults = totalResults,
                itemsPerPage = 0,
                startIndex = effectiveStartIndex,
                resources = emptyList()
            )
        }

        val effectiveCount = count ?: items.size
        val endIndex = minOf(zeroBasedStart + effectiveCount, items.size)
        val page = items.subList(zeroBasedStart, endIndex)

        return ListResponse(
            totalResults = totalResults,
            itemsPerPage = page.size,
            startIndex = effectiveStartIndex,
            resources = page
        )
    }
}
