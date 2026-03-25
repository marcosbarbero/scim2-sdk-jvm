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

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PathParserTest {

    private val faker = Faker()

    @Nested
    inner class SimplePathParsing {

        @Test
        fun `should parse simple attribute name`() {
            val node = PathParser.parse("userName")
            node.shouldBeInstanceOf<PathNode.SimplePath>()
            node.attributeName shouldBe "userName"
            node.subAttribute shouldBe null
        }

        @Test
        fun `should parse dotted attribute path`() {
            val node = PathParser.parse("name.familyName")
            node.shouldBeInstanceOf<PathNode.SimplePath>()
            node.attributeName shouldBe "name"
            node.subAttribute shouldBe "familyName"
        }
    }

    @Nested
    inner class FilteredPathParsing {

        @Test
        fun `should parse filtered path`() {
            val filterVal = faker.name.name()
            val node = PathParser.parse("emails[type eq \"$filterVal\"]")
            node.shouldBeInstanceOf<PathNode.FilteredPath>()
            node.attributeName shouldBe "emails"
            node.filter.shouldBeInstanceOf<AttributeExpression>()
            node.subAttribute shouldBe null
        }

        @Test
        fun `should parse filtered path with sub-attribute`() {
            val filterVal = faker.name.name()
            val node = PathParser.parse("emails[type eq \"$filterVal\"].value")
            node.shouldBeInstanceOf<PathNode.FilteredPath>()
            node.attributeName shouldBe "emails"
            node.subAttribute shouldBe "value"
        }
    }
}
