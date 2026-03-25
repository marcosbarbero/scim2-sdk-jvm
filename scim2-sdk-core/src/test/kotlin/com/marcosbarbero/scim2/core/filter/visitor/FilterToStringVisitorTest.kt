package com.marcosbarbero.scim2.core.filter.visitor

import com.marcosbarbero.scim2.core.filter.parser.FilterParser
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterToStringVisitorTest {

    private val visitor = FilterToStringVisitor()

    private fun roundTrip(filter: String): String {
        val node = FilterParser.parse(filter)
        return visitor.visit(node)
    }

    @Test
    fun `should round-trip simple equality`() {
        roundTrip("userName eq \"john\"") shouldBe "userName eq \"john\""
    }

    @Test
    fun `should round-trip presence`() {
        roundTrip("title pr") shouldBe "title pr"
    }

    @Test
    fun `should round-trip AND expression`() {
        roundTrip("userName eq \"john\" and active eq true") shouldBe
            "userName eq \"john\" and active eq true"
    }

    @Test
    fun `should round-trip OR expression`() {
        roundTrip("userName eq \"john\" or userName eq \"jane\"") shouldBe
            "userName eq \"john\" or userName eq \"jane\""
    }

    @Test
    fun `should round-trip NOT expression`() {
        roundTrip("not userName eq \"john\"") shouldBe "not userName eq \"john\""
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
        val result = roundTrip("a eq \"1\" or b eq \"2\" and c eq \"3\"")
        result shouldBe "a eq \"1\" or b eq \"2\" and c eq \"3\""
    }

    @Test
    fun `should round-trip dotted path`() {
        roundTrip("name.familyName eq \"Jensen\"") shouldBe "name.familyName eq \"Jensen\""
    }

    @Test
    fun `round-trip parse produces same AST`() {
        val filter = "userName eq \"john\" and active eq true"
        val ast1 = FilterParser.parse(filter)
        val serialized = visitor.visit(ast1)
        val ast2 = FilterParser.parse(serialized)
        ast1 shouldBe ast2
    }
}
