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
package com.marcosbarbero.scim2.client.dsl

import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.model.search.SortOrder
import com.marcosbarbero.scim2.core.filter.ast.FilterNode

fun scimSearch(block: SearchBuilder.() -> Unit): SearchRequest {
    val builder = SearchBuilder()
    builder.block()
    return builder.build()
}

class SearchBuilder {
    private var filter: String? = null
    private var sortBy: String? = null
    private var sortOrder: SortOrder = SortOrder.ASCENDING
    private var startIndex: Int = 1
    private var count: Int? = null
    private var attributes: List<String> = emptyList()
    private var excludedAttributes: List<String> = emptyList()

    fun filter(value: String) {
        this.filter = value
    }
    fun filter(block: FilterBuilder.() -> FilterNode) {
        this.filter = scimFilter(block).toFilterString()
    }
    fun sortBy(value: String) {
        this.sortBy = value
    }
    fun sortOrder(value: SortOrder) {
        this.sortOrder = value
    }
    fun startIndex(value: Int) {
        this.startIndex = value
    }
    fun count(value: Int) {
        this.count = value
    }
    fun attributes(vararg attrs: String) {
        this.attributes = attrs.toList()
    }
    fun excludedAttributes(vararg attrs: String) {
        this.excludedAttributes = attrs.toList()
    }

    fun build() = SearchRequest(
        filter = filter,
        sortBy = sortBy,
        sortOrder = sortOrder,
        startIndex = startIndex,
        count = count,
        attributes = attributes.ifEmpty { null },
        excludedAttributes = excludedAttributes.ifEmpty { null },
    )
}
