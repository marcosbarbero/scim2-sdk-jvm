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
package com.marcosbarbero.scim2.core.filter.lexer

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.error.InvalidFilterException
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FilterLexerTest {

    private val faker = Faker()

    private fun tokenize(input: String): List<Token> = FilterLexer(input).tokenize()

    private fun tokenTypes(input: String): List<TokenType> = tokenize(input).map { it.type }.dropLast(1) // drop EOF

    @Nested
    inner class AttributePathTokens {

        @Test
        fun `should tokenize simple attribute`() {
            val tokens = tokenize("userName")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.ATTR_PATH
            tokens[0].value shouldBe "userName"
            tokens[1].type shouldBe TokenType.EOF
        }

        @Test
        fun `should tokenize dotted attribute path`() {
            val tokens = tokenize("name.familyName")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.ATTR_PATH
            tokens[0].value shouldBe "name.familyName"
        }

        @Test
        fun `should tokenize URN-prefixed attribute path`() {
            val tokens = tokenize("${ScimUrns.ENTERPRISE_USER}:department")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.ATTR_PATH
            tokens[0].value shouldBe "${ScimUrns.ENTERPRISE_USER}:department"
        }
    }

    @Nested
    inner class LiteralTokens {

        @Test
        fun `should tokenize quoted string`() {
            val value = faker.name.name()
            val tokens = tokenize("\"$value\"")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.STRING_VALUE
            tokens[0].value shouldBe value
        }

        @Test
        fun `should tokenize string with escaped quote`() {
            val tokens = tokenize("\"hello \\\"world\\\"\"")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.STRING_VALUE
            tokens[0].value shouldBe "hello \"world\""
        }

        @Test
        fun `should tokenize string with escaped backslash`() {
            val tokens = tokenize("\"path\\\\to\"")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.STRING_VALUE
            tokens[0].value shouldBe "path\\to"
        }

        @Test
        fun `should tokenize integer number`() {
            val tokens = tokenize("42")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.NUMBER_VALUE
            tokens[0].value shouldBe "42"
        }

        @Test
        fun `should tokenize decimal number`() {
            val tokens = tokenize("3.14")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.NUMBER_VALUE
            tokens[0].value shouldBe "3.14"
        }

        @Test
        fun `should tokenize negative number`() {
            val tokens = tokenize("-99")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.NUMBER_VALUE
            tokens[0].value shouldBe "-99"
        }

        @Test
        fun `should tokenize boolean true`() {
            val tokens = tokenize("true")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.BOOLEAN_VALUE
            tokens[0].value shouldBe "true"
        }

        @Test
        fun `should tokenize boolean false`() {
            val tokens = tokenize("false")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.BOOLEAN_VALUE
            tokens[0].value shouldBe "false"
        }

        @Test
        fun `should tokenize null`() {
            val tokens = tokenize("null")
            tokens shouldHaveSize 2
            tokens[0].type shouldBe TokenType.NULL_VALUE
            tokens[0].value shouldBe "null"
        }
    }

    @Nested
    inner class ComparisonOperatorTokens {

        @Test
        fun `should tokenize all comparison operators case-insensitively`() {
            val operators = mapOf(
                "eq" to TokenType.OP_EQ,
                "NE" to TokenType.OP_NE,
                "Co" to TokenType.OP_CO,
                "sw" to TokenType.OP_SW,
                "EW" to TokenType.OP_EW,
                "gt" to TokenType.OP_GT,
                "GE" to TokenType.OP_GE,
                "lt" to TokenType.OP_LT,
                "LE" to TokenType.OP_LE,
                "pr" to TokenType.OP_PR,
            )
            for ((op, expectedType) in operators) {
                val tokens = tokenize("attr $op")
                tokens[1].type shouldBe expectedType
            }
        }
    }

    @Nested
    inner class LogicalOperatorTokens {

        @Test
        fun `should tokenize and`() {
            val val1 = faker.name.firstName()
            val val2 = faker.name.lastName()
            tokenTypes("a eq \"$val1\" and b eq \"$val2\"") shouldBe listOf(
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
                TokenType.OP_AND,
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
            )
        }

        @Test
        fun `should tokenize or`() {
            val val1 = faker.name.firstName()
            val val2 = faker.name.lastName()
            tokenTypes("a eq \"$val1\" or b eq \"$val2\"") shouldBe listOf(
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
                TokenType.OP_OR,
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
            )
        }

        @Test
        fun `should tokenize not`() {
            val value = faker.name.firstName()
            tokenTypes("not a eq \"$value\"") shouldBe listOf(
                TokenType.OP_NOT,
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
            )
        }
    }

    @Nested
    inner class GroupingTokens {

        @Test
        fun `should tokenize parentheses`() {
            val value = faker.name.firstName()
            tokenTypes("(a eq \"$value\")") shouldBe listOf(
                TokenType.LPAREN,
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
                TokenType.RPAREN,
            )
        }

        @Test
        fun `should tokenize brackets`() {
            val value = faker.name.name()
            tokenTypes("emails[type eq \"$value\"]") shouldBe listOf(
                TokenType.ATTR_PATH,
                TokenType.LBRACKET,
                TokenType.ATTR_PATH,
                TokenType.OP_EQ,
                TokenType.STRING_VALUE,
                TokenType.RBRACKET,
            )
        }
    }

    @Nested
    inner class ComplexFilters {

        @Test
        fun `should tokenize complex filter with multiple operators`() {
            val name = faker.name.firstName()
            tokenTypes("userName eq \"$name\" and active eq true or title pr") shouldBe listOf(
                TokenType.ATTR_PATH, TokenType.OP_EQ, TokenType.STRING_VALUE,
                TokenType.OP_AND,
                TokenType.ATTR_PATH, TokenType.OP_EQ, TokenType.BOOLEAN_VALUE,
                TokenType.OP_OR,
                TokenType.ATTR_PATH, TokenType.OP_PR,
            )
        }

        @Test
        fun `should tokenize value path filter`() {
            val domain = faker.internet.domain()
            tokenTypes("emails[type eq \"work\" and value co \"@$domain\"]") shouldBe listOf(
                TokenType.ATTR_PATH, TokenType.LBRACKET,
                TokenType.ATTR_PATH, TokenType.OP_EQ, TokenType.STRING_VALUE,
                TokenType.OP_AND,
                TokenType.ATTR_PATH, TokenType.OP_CO, TokenType.STRING_VALUE,
                TokenType.RBRACKET,
            )
        }
    }

    @Nested
    inner class ErrorHandling {

        @Test
        fun `should report error with position for unclosed string`() {
            val ex = shouldThrow<InvalidFilterException> {
                tokenize("\"unclosed string")
            }
            ex.detail shouldContain "position"
        }

        @Test
        fun `should report error for unexpected character`() {
            val ex = shouldThrow<InvalidFilterException> {
                tokenize("@invalid")
            }
            ex.detail shouldContain "position"
        }
    }
}
