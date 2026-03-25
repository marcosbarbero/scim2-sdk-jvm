package com.marcosbarbero.scim2.core.filter.visitor

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import com.marcosbarbero.scim2.core.filter.ast.FilterNode
import com.marcosbarbero.scim2.core.filter.ast.LogicalExpression
import com.marcosbarbero.scim2.core.filter.ast.NotExpression
import com.marcosbarbero.scim2.core.filter.ast.PresentExpression
import com.marcosbarbero.scim2.core.filter.ast.ValuePathExpression

interface FilterVisitor<T> {
    fun visit(node: AttributeExpression): T
    fun visit(node: PresentExpression): T
    fun visit(node: LogicalExpression): T
    fun visit(node: NotExpression): T
    fun visit(node: ValuePathExpression): T

    fun visit(node: FilterNode): T = when (node) {
        is AttributeExpression -> visit(node)
        is PresentExpression -> visit(node)
        is LogicalExpression -> visit(node)
        is NotExpression -> visit(node)
        is ValuePathExpression -> visit(node)
    }
}
