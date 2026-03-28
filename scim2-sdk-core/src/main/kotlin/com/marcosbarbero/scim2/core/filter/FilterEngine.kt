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
import tools.jackson.databind.ObjectMapper

class FilterEngine {

    companion object {
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
}
