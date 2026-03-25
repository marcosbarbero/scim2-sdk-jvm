package com.marcosbarbero.scim2.core.filter.visitor

import com.marcosbarbero.scim2.core.filter.ast.AttributeExpression
import com.marcosbarbero.scim2.core.filter.ast.ComparisonOperator
import com.marcosbarbero.scim2.core.filter.ast.LogicalExpression
import com.marcosbarbero.scim2.core.filter.ast.LogicalOperator
import com.marcosbarbero.scim2.core.filter.ast.NotExpression
import com.marcosbarbero.scim2.core.filter.ast.PresentExpression
import com.marcosbarbero.scim2.core.filter.ast.ScimValue
import com.marcosbarbero.scim2.core.filter.ast.ValuePathExpression

class FilterToPredicateVisitor : FilterVisitor<(Map<String, Any?>) -> Boolean> {

    override fun visit(node: AttributeExpression): (Map<String, Any?>) -> Boolean = { data ->
        val actual = resolvePath(data, node.path.toFullPath())
        compareValues(actual, node.operator, node.value)
    }

    override fun visit(node: PresentExpression): (Map<String, Any?>) -> Boolean = { data ->
        val actual = resolvePath(data, node.path.toFullPath())
        actual != null
    }

    override fun visit(node: LogicalExpression): (Map<String, Any?>) -> Boolean = { data ->
        when (node.operator) {
            LogicalOperator.AND -> visit(node.left)(data) && visit(node.right)(data)
            LogicalOperator.OR -> visit(node.left)(data) || visit(node.right)(data)
        }
    }

    override fun visit(node: NotExpression): (Map<String, Any?>) -> Boolean = { data ->
        !visit(node.operand)(data)
    }

    override fun visit(node: ValuePathExpression): (Map<String, Any?>) -> Boolean = { data ->
        val collection = data[node.attributePath]
        if (collection is List<*>) {
            val innerPredicate = visit(node.filter)
            val matchingItems = collection.filterIsInstance<Map<String, Any?>>().filter { innerPredicate(it) }
            node.subAttribute?.let { subAttr ->
                // There's a sub-attribute + outer comparison - but as a standalone value path,
                // just check that matching items exist with that sub-attribute
                matchingItems.any { it.containsKey(subAttr) }
            } ?: matchingItems.isNotEmpty()
        } else {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvePath(data: Map<String, Any?>, path: String): Any? {
        val parts = path.split(".")
        var current: Any? = data
        for (part in parts) {
            if (current is Map<*, *>) {
                current = (current as Map<String, Any?>)[part]
            } else {
                return null
            }
        }
        return current
    }

    private fun compareValues(actual: Any?, operator: ComparisonOperator, expected: ScimValue): Boolean {
        // Handle null comparisons
        if (expected is ScimValue.NullValue) {
            return when (operator) {
                ComparisonOperator.EQ -> actual == null
                ComparisonOperator.NE -> actual != null
                else -> false
            }
        }

        if (actual == null) return false

        return when (operator) {
            ComparisonOperator.EQ -> equalsValue(actual, expected)
            ComparisonOperator.NE -> !equalsValue(actual, expected)
            ComparisonOperator.CO -> stringOp(actual, expected) { a, e -> a.contains(e, ignoreCase = true) }
            ComparisonOperator.SW -> stringOp(actual, expected) { a, e -> a.startsWith(e, ignoreCase = true) }
            ComparisonOperator.EW -> stringOp(actual, expected) { a, e -> a.endsWith(e, ignoreCase = true) }
            ComparisonOperator.GT -> numericCompare(actual, expected) { it > 0 }
            ComparisonOperator.GE -> numericCompare(actual, expected) { it >= 0 }
            ComparisonOperator.LT -> numericCompare(actual, expected) { it < 0 }
            ComparisonOperator.LE -> numericCompare(actual, expected) { it <= 0 }
        }
    }

    private fun equalsValue(actual: Any?, expected: ScimValue): Boolean = when (expected) {
        is ScimValue.StringValue -> actual.toString().equals(expected.value, ignoreCase = true)
        is ScimValue.NumberValue -> toComparable(actual) == toComparable(expected.value)
        is ScimValue.BooleanValue -> actual == expected.value
        is ScimValue.NullValue -> actual == null
    }

    private fun stringOp(actual: Any?, expected: ScimValue, op: (String, String) -> Boolean): Boolean {
        if (expected !is ScimValue.StringValue) return false
        return op(actual.toString(), expected.value)
    }

    private fun numericCompare(actual: Any?, expected: ScimValue, check: (Int) -> Boolean): Boolean {
        if (expected !is ScimValue.NumberValue) return false
        val a = toComparable(actual) ?: return false
        val b = toComparable(expected.value) ?: return false
        @Suppress("UNCHECKED_CAST")
        return check((a as Comparable<Any>).compareTo(b))
    }

    private fun toComparable(value: Any?): Comparable<*>? = when (value) {
        is Int -> value.toLong()
        is Long -> value
        is Float -> value.toDouble()
        is Double -> value
        is Number -> value.toDouble()
        is String -> value
        is Boolean -> value
        else -> null
    }
}
