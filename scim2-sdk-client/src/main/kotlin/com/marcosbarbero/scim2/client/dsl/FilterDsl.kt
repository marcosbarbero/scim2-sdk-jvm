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
package com.marcosbarbero.scim2.client.dsl

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import com.marcosbarbero.scim2.core.filter.ast.AttributePath
import com.marcosbarbero.scim2.core.filter.ast.ComparisonOperator
import com.marcosbarbero.scim2.core.filter.ast.FilterNode
import com.marcosbarbero.scim2.core.filter.ast.LogicalExpression
import com.marcosbarbero.scim2.core.filter.ast.LogicalOperator
import com.marcosbarbero.scim2.core.filter.ast.NotExpression
import com.marcosbarbero.scim2.core.filter.ast.PresentExpression
import com.marcosbarbero.scim2.core.filter.ast.ScimValue
import com.marcosbarbero.scim2.core.filter.visitor.FilterToStringVisitor

fun scimFilter(block: FilterBuilder.() -> FilterNode): FilterNode = FilterBuilder().block()

fun FilterNode.toFilterString(): String = FilterToStringVisitor().visit(this)

class FilterBuilder {
    fun attr(name: String) = AttributeRef(name)

    val userName get() = attr("userName")
    val displayName get() = attr("displayName")
    val active get() = attr("active")
    val emails get() = attr("emails")
    val name get() = attr("name")
    val title get() = attr("title")
    val externalId get() = attr("externalId")

    data class AttributeRef(val name: String) {
        infix fun eq(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.EQ, ScimValue.StringValue(value))
        infix fun ne(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.NE, ScimValue.StringValue(value))
        infix fun co(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.CO, ScimValue.StringValue(value))
        infix fun sw(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.SW, ScimValue.StringValue(value))
        infix fun ew(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.EW, ScimValue.StringValue(value))
        infix fun gt(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.GT, ScimValue.StringValue(value))
        infix fun ge(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.GE, ScimValue.StringValue(value))
        infix fun lt(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.LT, ScimValue.StringValue(value))
        infix fun le(value: String) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.LE, ScimValue.StringValue(value))
        val pr get() = PresentExpression(AttributePath(attributeName = name))
        infix fun eq(value: Boolean) = AttributeExpression(AttributePath(attributeName = name), ComparisonOperator.EQ, ScimValue.BooleanValue(value))
    }

    infix fun FilterNode.and(other: FilterNode) = LogicalExpression(LogicalOperator.AND, this, other)
    infix fun FilterNode.or(other: FilterNode) = LogicalExpression(LogicalOperator.OR, this, other)
    fun not(node: FilterNode) = NotExpression(node)
}
