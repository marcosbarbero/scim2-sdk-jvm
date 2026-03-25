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
package com.marcosbarbero.scim2.core.filter.parser

import com.marcosbarbero.scim2.core.domain.model.error.InvalidFilterException
import com.marcosbarbero.scim2.core.filter.ast.FilterNode
import com.marcosbarbero.scim2.core.filter.lexer.FilterLexer
import com.marcosbarbero.scim2.core.filter.lexer.TokenType

sealed class PathNode {
    data class SimplePath(val attributeName: String, val subAttribute: String? = null) : PathNode()
    data class FilteredPath(val attributeName: String, val filter: FilterNode, val subAttribute: String? = null) : PathNode()
}

object PathParser {

    fun parse(path: String): PathNode {
        if (path.isBlank()) {
            throw InvalidFilterException("Path expression is empty at position 0")
        }

        val tokens = FilterLexer(path).tokenize()
        val attrToken = tokens[0]
        if (attrToken.type != TokenType.ATTR_PATH) {
            throw InvalidFilterException("Expected attribute path at position ${attrToken.position}")
        }

        // Check for bracket filter: attr[filter]
        if (tokens.size > 1 && tokens[1].type == TokenType.LBRACKET) {
            val attrName = attrToken.value
            // Re-parse the bracket content as a filter
            val bracketStart = path.indexOf('[')
            val bracketEnd = path.lastIndexOf(']')
            if (bracketEnd < 0) {
                throw InvalidFilterException("Unclosed bracket at position $bracketStart")
            }
            val filterStr = path.substring(bracketStart + 1, bracketEnd)
            val filter = FilterParser.parse(filterStr)

            // Check for .subAttr after ]
            val afterBracket = path.substring(bracketEnd + 1)
            val subAttr = if (afterBracket.startsWith(".") && afterBracket.length > 1) {
                afterBracket.substring(1)
            } else {
                null
            }

            return PathNode.FilteredPath(attrName, filter, subAttr)
        }

        // Simple path: attr or attr.subAttr
        val dotIdx = attrToken.value.indexOf('.')
        return if (dotIdx >= 0) {
            PathNode.SimplePath(
                attributeName = attrToken.value.substring(0, dotIdx),
                subAttribute = attrToken.value.substring(dotIdx + 1)
            )
        } else {
            PathNode.SimplePath(attributeName = attrToken.value)
        }
    }
}
