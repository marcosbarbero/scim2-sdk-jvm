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
package com.marcosbarbero.scim2.core.filter.visitor

import com.marcosbarbero.scim2.core.filter.parser.FilterParser
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FilterToPredicateVisitorTest {

    private val faker = Faker()
    private val visitor = FilterToPredicateVisitor()

    private fun matches(filter: String, data: Map<String, Any?>): Boolean {
        val node = FilterParser.parse(filter)
        val predicate = visitor.visit(node)
        return predicate(data)
    }

    private lateinit var userName: String
    private lateinit var title: String
    private lateinit var familyName: String
    private lateinit var givenName: String
    private lateinit var workEmail: String
    private lateinit var homeEmail: String
    private lateinit var sampleUser: Map<String, Any?>

    @BeforeEach
    fun setUp() {
        userName = faker.name.firstName().lowercase()
        title = faker.name.name()
        familyName = faker.name.lastName()
        givenName = faker.name.firstName()
        workEmail = faker.internet.email()
        homeEmail = faker.internet.email()
        sampleUser = mapOf(
            "userName" to userName,
            "active" to true,
            "title" to title,
            "age" to 30,
            "name" to mapOf("familyName" to familyName, "givenName" to givenName),
            "emails" to listOf(
                mapOf("type" to "work", "value" to workEmail, "primary" to true),
                mapOf("type" to "home", "value" to homeEmail, "primary" to false)
            )
        )
    }

    @Nested
    inner class ComparisonOperators {

        @Test
        fun `eq should match equal string`() {
            matches("userName eq \"$userName\"", sampleUser) shouldBe true
            matches("userName eq \"${faker.name.firstName()}nonexistent\"", sampleUser) shouldBe false
        }

        @Test
        fun `eq should be case-insensitive for strings per RFC 7644`() {
            matches("userName eq \"${userName.uppercase()}\"", sampleUser) shouldBe true
            matches("userName eq \"${userName.replaceFirstChar { it.uppercase() }}\"", sampleUser) shouldBe true
        }

        @Test
        fun `ne should match not equal`() {
            matches("userName ne \"${faker.name.firstName()}other\"", sampleUser) shouldBe true
            matches("userName ne \"$userName\"", sampleUser) shouldBe false
        }

        @Test
        fun `ne should be case-insensitive for strings per RFC 7644`() {
            matches("userName ne \"${userName.uppercase()}\"", sampleUser) shouldBe false
            matches("userName ne \"${faker.name.firstName()}OTHER\"", sampleUser) shouldBe true
        }

        @Test
        fun `co should match contains`() {
            // Use a substring of the actual userName
            val substring = if (userName.length > 2) userName.substring(1, userName.length - 1) else userName
            matches("userName co \"$substring\"", sampleUser) shouldBe true
            matches("userName co \"${java.util.UUID.randomUUID().toString()}\"", sampleUser) shouldBe false
        }

        @Test
        fun `sw should match starts with`() {
            val prefix = if (userName.length > 2) userName.substring(0, 3) else userName
            matches("userName sw \"$prefix\"", sampleUser) shouldBe true
            matches("userName sw \"${java.util.UUID.randomUUID().toString()}\"", sampleUser) shouldBe false
        }

        @Test
        fun `ew should match ends with`() {
            val suffix = if (userName.length > 2) userName.substring(userName.length - 3) else userName
            matches("userName ew \"$suffix\"", sampleUser) shouldBe true
            matches("userName ew \"${java.util.UUID.randomUUID().toString()}\"", sampleUser) shouldBe false
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
            matches("userName eq \"$userName\" and active eq true", sampleUser) shouldBe true
            matches("userName eq \"$userName\" and active eq false", sampleUser) shouldBe false
        }

        @Test
        fun `or should require at least one condition`() {
            matches("userName eq \"$userName\" or userName eq \"${faker.name.firstName()}other\"", sampleUser) shouldBe true
            matches("userName eq \"${java.util.UUID.randomUUID().toString()}\" or userName eq \"${java.util.UUID.randomUUID().toString()}\"", sampleUser) shouldBe false
        }

        @Test
        fun `not should negate condition`() {
            matches("not userName eq \"${faker.name.firstName()}other\"", sampleUser) shouldBe true
            matches("not userName eq \"$userName\"", sampleUser) shouldBe false
        }
    }

    @Nested
    inner class DottedPaths {

        @Test
        fun `should resolve dotted attribute path`() {
            matches("name.familyName eq \"$familyName\"", sampleUser) shouldBe true
            matches("name.familyName eq \"${faker.name.lastName()}Other\"", sampleUser) shouldBe false
        }
    }

    @Nested
    inner class ValuePathFilters {

        @Test
        fun `should filter multi-valued attribute`() {
            matches("emails[type eq \"work\"]", sampleUser) shouldBe true
            matches("emails[type eq \"${faker.name.name()}\"]", sampleUser) shouldBe false
        }

        @Test
        fun `should filter value path with sub-attribute`() {
            // emails[type eq "work"].value - checks that a matching email has a value sub-attribute
            matches("emails[type eq \"work\"].value", sampleUser) shouldBe true
            matches("emails[type eq \"${faker.name.name()}\"].value", sampleUser) shouldBe false
        }
    }
}
