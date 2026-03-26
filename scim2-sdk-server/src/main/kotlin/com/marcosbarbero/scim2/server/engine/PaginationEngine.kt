/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marcosbarbero.scim2.server.engine

import com.marcosbarbero.scim2.core.domain.model.search.ListResponse

class PaginationEngine {

    fun <T> paginate(
        items: List<T>,
        startIndex: Int,
        count: Int?,
        totalResults: Int = items.size,
    ): ListResponse<T> {
        val effectiveStartIndex = maxOf(startIndex, 1)
        val zeroBasedStart = effectiveStartIndex - 1

        if (zeroBasedStart >= items.size) {
            return ListResponse(
                totalResults = totalResults,
                itemsPerPage = 0,
                startIndex = effectiveStartIndex,
                resources = emptyList(),
            )
        }

        val effectiveCount = count ?: items.size
        val endIndex = minOf(zeroBasedStart + effectiveCount, items.size)
        val page = items.subList(zeroBasedStart, endIndex)

        return ListResponse(
            totalResults = totalResults,
            itemsPerPage = page.size,
            startIndex = effectiveStartIndex,
            resources = page,
        )
    }
}
