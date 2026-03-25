package com.marcosbarbero.scim2.core.filter.ast

sealed class FilterNode

data class AttributeExpression(
    val path: AttributePath,
    val operator: ComparisonOperator,
    val value: ScimValue
) : FilterNode()

data class PresentExpression(
    val path: AttributePath
) : FilterNode()

data class LogicalExpression(
    val operator: LogicalOperator,
    val left: FilterNode,
    val right: FilterNode
) : FilterNode()

data class NotExpression(
    val operand: FilterNode
) : FilterNode()

data class ValuePathExpression(
    val attributePath: String,
    val filter: FilterNode,
    val subAttribute: String? = null
) : FilterNode()
