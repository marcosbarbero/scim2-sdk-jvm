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

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import com.marcosbarbero.scim2.core.filter.ast.LogicalExpression
import com.marcosbarbero.scim2.core.filter.ast.LogicalOperator
import com.marcosbarbero.scim2.core.filter.ast.NotExpression
import com.marcosbarbero.scim2.core.filter.ast.PresentExpression
import com.marcosbarbero.scim2.core.filter.ast.ScimValue
import com.marcosbarbero.scim2.core.filter.ast.ValuePathExpression

class FilterToStringVisitor : FilterVisitor<String> {

    override fun visit(node: AttributeExpression): String {
        val path = node.path.toFullPath()
        val op = node.operator.name.lowercase()
        val value = formatValue(node.value)
        return "$path $op $value"
    }

    override fun visit(node: PresentExpression): String = "${node.path.toFullPath()} pr"

    override fun visit(node: LogicalExpression): String {
        val left = visitWithPrecedence(node.left, node.operator)
        val right = visitWithPrecedence(node.right, node.operator, isRight = true)
        val op = node.operator.name.lowercase()
        return "$left $op $right"
    }

    override fun visit(node: NotExpression): String {
        val operand = visit(node.operand)
        return "not $operand"
    }

    override fun visit(node: ValuePathExpression): String {
        val inner = visit(node.filter)
        val sub = if (node.subAttribute != null) ".${node.subAttribute}" else ""
        return "${node.attributePath}[$inner]$sub"
    }

    private fun visitWithPrecedence(node: com.marcosbarbero.scim2.core.filter.ast.FilterNode, parentOp: LogicalOperator, isRight: Boolean = false): String {
        if (node is LogicalExpression && node.operator == LogicalOperator.OR && parentOp == LogicalOperator.AND) {
            return "(${visit(node)})"
        }
        return visit(node)
    }

    private fun formatValue(value: ScimValue): String = when (value) {
        is ScimValue.StringValue -> "\"${value.value}\""
        is ScimValue.NumberValue -> {
            if (value.value is Double || value.value is Float) {
                val d = value.value.toDouble()
                if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            } else {
                value.value.toString()
            }
        }
        is ScimValue.BooleanValue -> value.value.toString()
        is ScimValue.NullValue -> "null"
    }
}
