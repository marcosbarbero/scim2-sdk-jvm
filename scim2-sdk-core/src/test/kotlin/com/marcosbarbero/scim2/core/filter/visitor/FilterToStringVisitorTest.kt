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
import org.junit.jupiter.api.Test

class FilterToStringVisitorTest {

    private val faker = Faker()
    private val visitor = FilterToStringVisitor()

    private fun roundTrip(filter: String): String {
        val node = FilterParser.parse(filter)
        return visitor.visit(node)
    }

    @Test
    fun `should round-trip simple equality`() {
        val name = faker.name.firstName()
        roundTrip("userName eq \"$name\"") shouldBe "userName eq \"$name\""
    }

    @Test
    fun `should round-trip presence`() {
        roundTrip("title pr") shouldBe "title pr"
    }

    @Test
    fun `should round-trip AND expression`() {
        val name = faker.name.firstName()
        roundTrip("userName eq \"$name\" and active eq true") shouldBe
            "userName eq \"$name\" and active eq true"
    }

    @Test
    fun `should round-trip OR expression`() {
        val name1 = faker.name.firstName()
        val name2 = faker.name.firstName()
        roundTrip("userName eq \"$name1\" or userName eq \"$name2\"") shouldBe
            "userName eq \"$name1\" or userName eq \"$name2\""
    }

    @Test
    fun `should round-trip NOT expression`() {
        val name = faker.name.firstName()
        roundTrip("not userName eq \"$name\"") shouldBe "not userName eq \"$name\""
    }

    @Test
    fun `should round-trip value path`() {
        roundTrip("emails[type eq \"work\"]") shouldBe "emails[type eq \"work\"]"
    }

    @Test
    fun `should round-trip value path with sub-attribute`() {
        roundTrip("emails[type eq \"work\"].value") shouldBe "emails[type eq \"work\"].value"
    }

    @Test
    fun `should round-trip number comparison`() {
        roundTrip("age gt 30") shouldBe "age gt 30"
    }

    @Test
    fun `should round-trip boolean comparison`() {
        roundTrip("active eq true") shouldBe "active eq true"
    }

    @Test
    fun `should round-trip null comparison`() {
        roundTrip("title eq null") shouldBe "title eq null"
    }

    @Test
    fun `should round-trip complex precedence`() {
        // Parser produces OR(a=1, AND(b=2, c=3)), which serializes without extra parens
        val val1 = faker.name.name()
        val val2 = faker.name.name()
        val val3 = faker.name.name()
        val result = roundTrip("a eq \"$val1\" or b eq \"$val2\" and c eq \"$val3\"")
        result shouldBe "a eq \"$val1\" or b eq \"$val2\" and c eq \"$val3\""
    }

    @Test
    fun `should round-trip dotted path`() {
        val familyName = faker.name.lastName()
        roundTrip("name.familyName eq \"$familyName\"") shouldBe "name.familyName eq \"$familyName\""
    }

    @Test
    fun `round-trip parse produces same AST`() {
        val name = faker.name.firstName()
        val filter = "userName eq \"$name\" and active eq true"
        val ast1 = FilterParser.parse(filter)
        val serialized = visitor.visit(ast1)
        val ast2 = FilterParser.parse(serialized)
        ast1 shouldBe ast2
    }
}
