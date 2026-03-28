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
package com.marcosbarbero.scim2.core.filter

import com.marcosbarbero.scim2.core.filter.parser.FilterParser
import com.marcosbarbero.scim2.core.filter.visitor.FilterToPredicateVisitor
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper

/**
 * In-memory SCIM filter engine that evaluates RFC 7644 filter expressions against resource lists.
 *
 * **Known limitations of the in-memory filter approach:**
 * - Dot-path filters (e.g., `name.familyName eq "Smith"`) only work when the resource is serialized
 *   with nested map structure. Flattened representations will not match dot-path filters.
 * - Extension attributes (e.g., `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department`)
 *   are not resolved through the extension schema URI; they must appear as top-level keys in the
 *   serialized map for the filter to match.
 * - All filtering is performed in memory after loading resources. For large datasets, consider
 *   implementing SQL-level filtering in a custom [com.marcosbarbero.scim2.server.port.ResourceRepository].
 */
object FilterEngine {

    private val logger = LoggerFactory.getLogger(FilterEngine::class.java)

    @JvmStatic
    fun <T : Any> filter(resources: List<T>, filterString: String?, objectMapper: ObjectMapper): List<T> {
        if (filterString.isNullOrBlank()) return resources
        val ast = FilterParser.parse(filterString)
        val predicate = FilterToPredicateVisitor().visit(ast)
        return resources.filter { resource ->
            @Suppress("UNCHECKED_CAST")
            val map = objectMapper.convertValue(resource, Map::class.java) as Map<String, Any?>
            predicate(map)
        }
    }
}
