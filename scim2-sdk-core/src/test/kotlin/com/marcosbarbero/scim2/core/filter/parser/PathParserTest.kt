package com.marcosbarbero.scim2.core.filter.parser

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PathParserTest {

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
            val node = PathParser.parse("emails[type eq \"work\"]")
            node.shouldBeInstanceOf<PathNode.FilteredPath>()
            node.attributeName shouldBe "emails"
            node.filter.shouldBeInstanceOf<AttributeExpression>()
            node.subAttribute shouldBe null
        }

        @Test
        fun `should parse filtered path with sub-attribute`() {
            val node = PathParser.parse("emails[type eq \"work\"].value")
            node.shouldBeInstanceOf<PathNode.FilteredPath>()
            node.attributeName shouldBe "emails"
            node.subAttribute shouldBe "value"
        }
    }
}
