package com.marcosbarbero.scim2.client.dsl

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import com.marcosbarbero.scim2.core.filter.ast.AttributePath
import com.marcosbarbero.scim2.core.filter.ast.ComparisonOperator
import com.marcosbarbero.scim2.core.filter.ast.LogicalExpression
import com.marcosbarbero.scim2.core.filter.ast.LogicalOperator
import com.marcosbarbero.scim2.core.filter.ast.NotExpression
import com.marcosbarbero.scim2.core.filter.ast.PresentExpression
import com.marcosbarbero.scim2.core.filter.ast.ScimValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class FilterDslTest {

    @Test
    fun `scimFilter with eq produces correct AttributeExpression`() {
        val node = scimFilter { userName eq "john" }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.path shouldBe AttributePath(attributeName = "userName")
        node.operator shouldBe ComparisonOperator.EQ
        node.value shouldBe ScimValue.StringValue("john")
    }

    @Test
    fun `scimFilter with ne produces correct AttributeExpression`() {
        val node = scimFilter { displayName ne "admin" }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.operator shouldBe ComparisonOperator.NE
    }

    @Test
    fun `scimFilter with sw produces correct AttributeExpression`() {
        val node = scimFilter { userName sw "j" }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.operator shouldBe ComparisonOperator.SW
        node.value shouldBe ScimValue.StringValue("j")
    }

    @Test
    fun `scimFilter with co produces correct AttributeExpression`() {
        val node = scimFilter { userName co "oh" }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.operator shouldBe ComparisonOperator.CO
    }

    @Test
    fun `scimFilter with ew produces correct AttributeExpression`() {
        val node = scimFilter { userName ew "hn" }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.operator shouldBe ComparisonOperator.EW
    }

    @Test
    fun `scimFilter with gt ge lt le produces correct expressions`() {
        scimFilter { attr("age") gt "30" }.shouldBeInstanceOf<AttributeExpression>().operator shouldBe ComparisonOperator.GT
        scimFilter { attr("age") ge "30" }.shouldBeInstanceOf<AttributeExpression>().operator shouldBe ComparisonOperator.GE
        scimFilter { attr("age") lt "30" }.shouldBeInstanceOf<AttributeExpression>().operator shouldBe ComparisonOperator.LT
        scimFilter { attr("age") le "30" }.shouldBeInstanceOf<AttributeExpression>().operator shouldBe ComparisonOperator.LE
    }

    @Test
    fun `scimFilter with boolean eq produces correct expression`() {
        val node = scimFilter { active eq true }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.operator shouldBe ComparisonOperator.EQ
        node.value shouldBe ScimValue.BooleanValue(true)
    }

    @Test
    fun `scimFilter with pr produces PresentExpression`() {
        val node = scimFilter { title.pr }
        node.shouldBeInstanceOf<PresentExpression>()
        node.path shouldBe AttributePath(attributeName = "title")
    }

    @Test
    fun `scimFilter with and produces LogicalExpression`() {
        val node = scimFilter { (userName eq "john") and (active eq true) }
        node.shouldBeInstanceOf<LogicalExpression>()
        node.operator shouldBe LogicalOperator.AND
    }

    @Test
    fun `scimFilter with or produces LogicalExpression`() {
        val node = scimFilter { (userName eq "john") or (userName eq "jane") }
        node.shouldBeInstanceOf<LogicalExpression>()
        node.operator shouldBe LogicalOperator.OR
    }

    @Test
    fun `scimFilter with not produces NotExpression`() {
        val node = scimFilter { not(active eq true) }
        node.shouldBeInstanceOf<NotExpression>()
        node.operand.shouldBeInstanceOf<AttributeExpression>()
    }

    @Test
    fun `toFilterString produces correct string for eq`() {
        scimFilter { userName eq "john" }.toFilterString() shouldBe """userName eq "john""""
    }

    @Test
    fun `toFilterString produces correct string for and`() {
        scimFilter { (userName eq "john") and (active eq true) }.toFilterString() shouldBe """userName eq "john" and active eq true"""
    }

    @Test
    fun `toFilterString produces correct string for pr`() {
        scimFilter { title.pr }.toFilterString() shouldBe "title pr"
    }

    @Test
    fun `toFilterString produces correct string for not`() {
        scimFilter { not(active eq true) }.toFilterString() shouldBe "not active eq true"
    }

    @Test
    fun `custom attr produces correct path`() {
        val node = scimFilter { attr("urn:custom:attr") eq "value" }
        node.shouldBeInstanceOf<AttributeExpression>()
        node.path shouldBe AttributePath(attributeName = "urn:custom:attr")
    }
}
