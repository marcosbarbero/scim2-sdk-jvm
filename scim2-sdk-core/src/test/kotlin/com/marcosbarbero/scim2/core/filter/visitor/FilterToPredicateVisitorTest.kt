package com.marcosbarbero.scim2.core.filter.visitor

import com.marcosbarbero.scim2.core.filter.parser.FilterParser
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FilterToPredicateVisitorTest {

    private val visitor = FilterToPredicateVisitor()

    private fun matches(filter: String, data: Map<String, Any?>): Boolean {
        val node = FilterParser.parse(filter)
        val predicate = visitor.visit(node)
        return predicate(data)
    }

    private val sampleUser = mapOf<String, Any?>(
        "userName" to "bjensen",
        "active" to true,
        "title" to "Tour Guide",
        "age" to 30,
        "name" to mapOf("familyName" to "Jensen", "givenName" to "Barbara"),
        "emails" to listOf(
            mapOf("type" to "work", "value" to "bjensen@example.com", "primary" to true),
            mapOf("type" to "home", "value" to "babs@example.org", "primary" to false)
        )
    )

    @Nested
    inner class ComparisonOperators {

        @Test
        fun `eq should match equal string`() {
            matches("userName eq \"bjensen\"", sampleUser) shouldBe true
            matches("userName eq \"other\"", sampleUser) shouldBe false
        }

        @Test
        fun `ne should match not equal`() {
            matches("userName ne \"other\"", sampleUser) shouldBe true
            matches("userName ne \"bjensen\"", sampleUser) shouldBe false
        }

        @Test
        fun `co should match contains`() {
            matches("userName co \"jensen\"", sampleUser) shouldBe true
            matches("userName co \"xyz\"", sampleUser) shouldBe false
        }

        @Test
        fun `sw should match starts with`() {
            matches("userName sw \"bjen\"", sampleUser) shouldBe true
            matches("userName sw \"xyz\"", sampleUser) shouldBe false
        }

        @Test
        fun `ew should match ends with`() {
            matches("userName ew \"ensen\"", sampleUser) shouldBe true
            matches("userName ew \"xyz\"", sampleUser) shouldBe false
        }

        @Test
        fun `gt should match greater than`() {
            matches("age gt 25", sampleUser) shouldBe true
            matches("age gt 30", sampleUser) shouldBe false
        }

        @Test
        fun `ge should match greater than or equal`() {
            matches("age ge 30", sampleUser) shouldBe true
            matches("age ge 31", sampleUser) shouldBe false
        }

        @Test
        fun `lt should match less than`() {
            matches("age lt 35", sampleUser) shouldBe true
            matches("age lt 30", sampleUser) shouldBe false
        }

        @Test
        fun `le should match less than or equal`() {
            matches("age le 30", sampleUser) shouldBe true
            matches("age le 29", sampleUser) shouldBe false
        }

        @Test
        fun `eq should match boolean`() {
            matches("active eq true", sampleUser) shouldBe true
            matches("active eq false", sampleUser) shouldBe false
        }

        @Test
        fun `eq null should match missing attribute`() {
            matches("missing eq null", sampleUser) shouldBe true
            matches("userName eq null", sampleUser) shouldBe false
        }
    }

    @Nested
    inner class PresenceOperator {

        @Test
        fun `pr should match present attribute`() {
            matches("title pr", sampleUser) shouldBe true
        }

        @Test
        fun `pr should not match absent attribute`() {
            matches("missing pr", sampleUser) shouldBe false
        }
    }

    @Nested
    inner class LogicalOperators {

        @Test
        fun `and should require both conditions`() {
            matches("userName eq \"bjensen\" and active eq true", sampleUser) shouldBe true
            matches("userName eq \"bjensen\" and active eq false", sampleUser) shouldBe false
        }

        @Test
        fun `or should require at least one condition`() {
            matches("userName eq \"bjensen\" or userName eq \"other\"", sampleUser) shouldBe true
            matches("userName eq \"x\" or userName eq \"y\"", sampleUser) shouldBe false
        }

        @Test
        fun `not should negate condition`() {
            matches("not userName eq \"other\"", sampleUser) shouldBe true
            matches("not userName eq \"bjensen\"", sampleUser) shouldBe false
        }
    }

    @Nested
    inner class DottedPaths {

        @Test
        fun `should resolve dotted attribute path`() {
            matches("name.familyName eq \"Jensen\"", sampleUser) shouldBe true
            matches("name.familyName eq \"Smith\"", sampleUser) shouldBe false
        }
    }

    @Nested
    inner class ValuePathFilters {

        @Test
        fun `should filter multi-valued attribute`() {
            matches("emails[type eq \"work\"]", sampleUser) shouldBe true
            matches("emails[type eq \"other\"]", sampleUser) shouldBe false
        }

        @Test
        fun `should filter value path with sub-attribute`() {
            // emails[type eq "work"].value - checks that a matching email has a value sub-attribute
            matches("emails[type eq \"work\"].value", sampleUser) shouldBe true
            matches("emails[type eq \"other\"].value", sampleUser) shouldBe false
        }
    }
}
