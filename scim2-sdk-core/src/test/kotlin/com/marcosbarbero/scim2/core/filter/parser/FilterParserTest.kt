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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FilterParserTest {

    private fun parse(filter: String): FilterNode = FilterParser.parse(filter)

    @Nested
    inner class SimpleComparisons {

        @Test
        fun `should parse string equality`() {
            val node = parse("userName eq \"john\"")
            node.shouldBeInstanceOf<AttributeExpression>()
            node.path shouldBe AttributePath(attributeName = "userName")
            node.operator shouldBe ComparisonOperator.EQ
            node.value shouldBe ScimValue.StringValue("john")
        }

        @Test
        fun `should parse all comparison operators`() {
            val ops = mapOf(
                "eq" to ComparisonOperator.EQ,
                "ne" to ComparisonOperator.NE,
                "co" to ComparisonOperator.CO,
                "sw" to ComparisonOperator.SW,
                "ew" to ComparisonOperator.EW,
                "gt" to ComparisonOperator.GT,
                "ge" to ComparisonOperator.GE,
                "lt" to ComparisonOperator.LT,
                "le" to ComparisonOperator.LE
            )
            for ((op, expected) in ops) {
                val node = parse("attr $op \"val\"")
                node.shouldBeInstanceOf<AttributeExpression>()
                node.operator shouldBe expected
            }
        }

        @Test
        fun `should parse number comparison`() {
            val node = parse("age gt 30")
            node.shouldBeInstanceOf<AttributeExpression>()
            node.value.shouldBeInstanceOf<ScimValue.NumberValue>()
            (node.value as ScimValue.NumberValue).value shouldBe 30
        }

        @Test
        fun `should parse decimal number comparison`() {
            val node = parse("score ge 3.14")
            node.shouldBeInstanceOf<AttributeExpression>()
            val value = node.value as ScimValue.NumberValue
            value.value shouldBe 3.14
        }

        @Test
        fun `should parse boolean comparison`() {
            val node = parse("active eq true")
            node.shouldBeInstanceOf<AttributeExpression>()
            node.value shouldBe ScimValue.BooleanValue(true)
        }

        @Test
        fun `should parse null comparison`() {
            val node = parse("title eq null")
            node.shouldBeInstanceOf<AttributeExpression>()
            node.value shouldBe ScimValue.NullValue
        }

        @Test
        fun `should parse dotted attribute path`() {
            val node = parse("name.familyName eq \"Jensen\"")
            node.shouldBeInstanceOf<AttributeExpression>()
            node.path shouldBe AttributePath(attributeName = "name", subAttribute = "familyName")
        }
    }

    @Nested
    inner class PresenceFilter {

        @Test
        fun `should parse presence filter`() {
            val node = parse("title pr")
            node.shouldBeInstanceOf<PresentExpression>()
            node.path shouldBe AttributePath(attributeName = "title")
        }

        @Test
        fun `should parse presence filter with dotted path`() {
            val node = parse("name.familyName pr")
            node.shouldBeInstanceOf<PresentExpression>()
            node.path shouldBe AttributePath(attributeName = "name", subAttribute = "familyName")
        }
    }

    @Nested
    inner class LogicalOperators {

        @Test
        fun `should parse AND expression`() {
            val node = parse("userName eq \"john\" and active eq true")
            node.shouldBeInstanceOf<LogicalExpression>()
            node.operator shouldBe LogicalOperator.AND
            node.left.shouldBeInstanceOf<AttributeExpression>()
            node.right.shouldBeInstanceOf<AttributeExpression>()
        }

        @Test
        fun `should parse OR expression`() {
            val node = parse("userName eq \"john\" or userName eq \"jane\"")
            node.shouldBeInstanceOf<LogicalExpression>()
            node.operator shouldBe LogicalOperator.OR
        }

        @Test
        fun `should handle AND higher precedence than OR`() {
            // a eq "1" or b eq "2" and c eq "3" → OR(a=1, AND(b=2, c=3))
            val node = parse("a eq \"1\" or b eq \"2\" and c eq \"3\"")
            node.shouldBeInstanceOf<LogicalExpression>()
            node.operator shouldBe LogicalOperator.OR
            node.left.shouldBeInstanceOf<AttributeExpression>()
            (node.left as AttributeExpression).path.attributeName shouldBe "a"
            node.right.shouldBeInstanceOf<LogicalExpression>()
            (node.right as LogicalExpression).operator shouldBe LogicalOperator.AND
        }

        @Test
        fun `should parse NOT expression`() {
            val node = parse("not userName eq \"john\"")
            node.shouldBeInstanceOf<NotExpression>()
            node.operand.shouldBeInstanceOf<AttributeExpression>()
        }

        @Test
        fun `should parse NOT with higher precedence than AND`() {
            // not a eq "1" and b eq "2" → AND(NOT(a=1), b=2)
            val node = parse("not a eq \"1\" and b eq \"2\"")
            node.shouldBeInstanceOf<LogicalExpression>()
            node.operator shouldBe LogicalOperator.AND
            node.left.shouldBeInstanceOf<NotExpression>()
            node.right.shouldBeInstanceOf<AttributeExpression>()
        }
    }

    @Nested
    inner class Grouping {

        @Test
        fun `should parse parenthesized expression`() {
            val node = parse("(userName eq \"john\")")
            node.shouldBeInstanceOf<AttributeExpression>()
        }

        @Test
        fun `should override precedence with parentheses`() {
            // (a eq "1" or b eq "2") and c eq "3" → AND(OR(a=1, b=2), c=3)
            val node = parse("(a eq \"1\" or b eq \"2\") and c eq \"3\"")
            node.shouldBeInstanceOf<LogicalExpression>()
            node.operator shouldBe LogicalOperator.AND
            node.left.shouldBeInstanceOf<LogicalExpression>()
            (node.left as LogicalExpression).operator shouldBe LogicalOperator.OR
        }
    }

    @Nested
    inner class ValuePathFilters {

        @Test
        fun `should parse simple value path`() {
            val node = parse("emails[type eq \"work\"]")
            node.shouldBeInstanceOf<ValuePathExpression>()
            node.attributePath shouldBe "emails"
            node.filter.shouldBeInstanceOf<AttributeExpression>()
            node.subAttribute shouldBe null
        }

        @Test
        fun `should parse value path with sub-attribute`() {
            val node = parse("emails[type eq \"work\"].value")
            node.shouldBeInstanceOf<ValuePathExpression>()
            node.attributePath shouldBe "emails"
            node.subAttribute shouldBe "value"
        }

        @Test
        fun `should parse value path with complex inner filter`() {
            val node = parse("emails[type eq \"work\" and value co \"@example.com\"]")
            node.shouldBeInstanceOf<ValuePathExpression>()
            node.filter.shouldBeInstanceOf<LogicalExpression>()
        }
    }

    @Nested
    inner class UrnPrefixedPaths {

        @Test
        fun `should parse URN-prefixed attribute path`() {
            val node = parse("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department eq \"Engineering\"")
            node.shouldBeInstanceOf<AttributeExpression>()
            node.path.schemaUri shouldBe "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
            node.path.attributeName shouldBe "department"
        }
    }

    @Nested
    inner class ErrorCases {

        @Test
        fun `should throw on missing comparison value`() {
            val ex = shouldThrow<InvalidFilterException> {
                parse("userName eq")
            }
            ex.detail shouldContain "position"
        }

        @Test
        fun `should throw on unclosed parenthesis`() {
            val ex = shouldThrow<InvalidFilterException> {
                parse("(userName eq \"john\"")
            }
            ex.detail shouldContain "position"
        }

        @Test
        fun `should throw on invalid operator`() {
            val ex = shouldThrow<InvalidFilterException> {
                parse("userName xx \"john\"")
            }
            ex.detail shouldContain "position"
        }

        @Test
        fun `should throw on empty filter`() {
            shouldThrow<InvalidFilterException> {
                parse("")
            }
        }

        @Test
        fun `should throw on unclosed bracket`() {
            val ex = shouldThrow<InvalidFilterException> {
                parse("emails[type eq \"work\"")
            }
            ex.detail shouldContain "position"
        }
    }
}
