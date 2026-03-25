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
import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import com.marcosbarbero.scim2.core.filter.ast.AttributePath
import com.marcosbarbero.scim2.core.filter.ast.ComparisonOperator
import com.marcosbarbero.scim2.core.filter.ast.FilterNode
import com.marcosbarbero.scim2.core.filter.ast.LogicalExpression
import com.marcosbarbero.scim2.core.filter.ast.LogicalOperator
import com.marcosbarbero.scim2.core.filter.ast.NotExpression
import com.marcosbarbero.scim2.core.filter.ast.PresentExpression
import com.marcosbarbero.scim2.core.filter.ast.ScimValue
import com.marcosbarbero.scim2.core.filter.ast.ValuePathExpression
import com.marcosbarbero.scim2.core.filter.lexer.FilterLexer
import com.marcosbarbero.scim2.core.filter.lexer.Token
import com.marcosbarbero.scim2.core.filter.lexer.TokenType

object FilterParser {

    fun parse(filter: String): FilterNode {
        if (filter.isBlank()) {
            throw InvalidFilterException("Filter expression is empty at position 0")
        }
        val tokens = FilterLexer(filter).tokenize()
        val parser = Parser(tokens)
        val node = parser.parseOrExpression()
        parser.expect(TokenType.EOF, "Unexpected token after filter expression")
        return node
    }

    internal class Parser(private val tokens: List<Token>) {
        private var pos = 0

        private fun current(): Token = tokens[pos]

        private fun advance(): Token {
            val token = tokens[pos]
            if (pos < tokens.size - 1) pos++
            return token
        }

        private fun peek(): TokenType = current().type

        fun expect(type: TokenType, message: String): Token {
            if (peek() != type) {
                throw InvalidFilterException("$message at position ${current().position}")
            }
            return advance()
        }

        fun parseOrExpression(): FilterNode {
            var left = parseAndExpression()
            while (peek() == TokenType.OP_OR) {
                advance()
                val right = parseAndExpression()
                left = LogicalExpression(LogicalOperator.OR, left, right)
            }
            return left
        }

        private fun parseAndExpression(): FilterNode {
            var left = parseNotExpression()
            while (peek() == TokenType.OP_AND) {
                advance()
                val right = parseNotExpression()
                left = LogicalExpression(LogicalOperator.AND, left, right)
            }
            return left
        }

        private fun parseNotExpression(): FilterNode {
            if (peek() == TokenType.OP_NOT) {
                advance()
                val operand = parseNotExpression()
                return NotExpression(operand)
            }
            return parsePrimaryExpression()
        }

        private fun parsePrimaryExpression(): FilterNode {
            if (peek() == TokenType.LPAREN) {
                advance()
                val expr = parseOrExpression()
                expect(TokenType.RPAREN, "Expected ')' to close grouping")
                return expr
            }
            return parseAttrExpression()
        }

        private fun parseAttrExpression(): FilterNode {
            val pathToken = expect(TokenType.ATTR_PATH, "Expected attribute path")
            val path = parseAttributePath(pathToken.value)

            // Check for value path: attr[filter]
            if (peek() == TokenType.LBRACKET) {
                advance()
                val innerFilter = parseOrExpression()
                expect(TokenType.RBRACKET, "Expected ']' to close value path filter")

                // Check for .subAttr after ]
                var subAttr: String? = null
                if (peek() == TokenType.ATTR_PATH && current().value.startsWith(".")) {
                    subAttr = advance().value.removePrefix(".")
                }
                return ValuePathExpression(pathToken.value, innerFilter, subAttr)
            }

            // Check for presence
            if (peek() == TokenType.OP_PR) {
                advance()
                return PresentExpression(path)
            }

            // Must be comparison
            val operator = parseComparisonOperator()
            val value = parseValue()
            return AttributeExpression(path, operator, value)
        }

        private fun parseAttributePath(raw: String): AttributePath {
            // Check for URN prefix: urn:...:attrName or urn:...:attrName.subAttr
            if (raw.startsWith("urn:")) {
                val lastColon = raw.lastIndexOf(':')
                val schemaUri = raw.substring(0, lastColon)
                val remainder = raw.substring(lastColon + 1)
                val dotIdx = remainder.indexOf('.')
                return if (dotIdx >= 0) {
                    AttributePath(
                        schemaUri = schemaUri,
                        attributeName = remainder.substring(0, dotIdx),
                        subAttribute = remainder.substring(dotIdx + 1)
                    )
                } else {
                    AttributePath(schemaUri = schemaUri, attributeName = remainder)
                }
            }

            // Simple or dotted path
            val dotIdx = raw.indexOf('.')
            return if (dotIdx >= 0) {
                AttributePath(
                    attributeName = raw.substring(0, dotIdx),
                    subAttribute = raw.substring(dotIdx + 1)
                )
            } else {
                AttributePath(attributeName = raw)
            }
        }

        private fun parseComparisonOperator(): ComparisonOperator {
            val token = current()
            val op = when (token.type) {
                TokenType.OP_EQ -> ComparisonOperator.EQ
                TokenType.OP_NE -> ComparisonOperator.NE
                TokenType.OP_CO -> ComparisonOperator.CO
                TokenType.OP_SW -> ComparisonOperator.SW
                TokenType.OP_EW -> ComparisonOperator.EW
                TokenType.OP_GT -> ComparisonOperator.GT
                TokenType.OP_GE -> ComparisonOperator.GE
                TokenType.OP_LT -> ComparisonOperator.LT
                TokenType.OP_LE -> ComparisonOperator.LE
                else -> throw InvalidFilterException(
                    "Expected comparison operator but found '${token.value}' at position ${token.position}"
                )
            }
            advance()
            return op
        }

        private fun parseValue(): ScimValue {
            val token = current()
            return when (token.type) {
                TokenType.STRING_VALUE -> {
                    advance()
                    ScimValue.StringValue(token.value)
                }
                TokenType.NUMBER_VALUE -> {
                    advance()
                    val num = if (token.value.contains('.')) {
                        token.value.toDouble()
                    } else {
                        token.value.toLong()
                    }
                    ScimValue.NumberValue(num)
                }
                TokenType.BOOLEAN_VALUE -> {
                    advance()
                    ScimValue.BooleanValue(token.value.toBoolean())
                }
                TokenType.NULL_VALUE -> {
                    advance()
                    ScimValue.NullValue
                }
                else -> throw InvalidFilterException(
                    "Expected value but found '${token.value}' at position ${token.position}"
                )
            }
        }
    }
}
