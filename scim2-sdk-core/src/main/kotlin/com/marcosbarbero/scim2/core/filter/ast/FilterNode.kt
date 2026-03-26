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
package com.marcosbarbero.scim2.core.filter.ast

sealed class FilterNode

data class AttributeExpression(
    val path: AttributePath,
    val operator: ComparisonOperator,
    val value: ScimValue,
) : FilterNode()

data class PresentExpression(
    val path: AttributePath,
) : FilterNode()

data class LogicalExpression(
    val operator: LogicalOperator,
    val left: FilterNode,
    val right: FilterNode,
) : FilterNode()

data class NotExpression(
    val operand: FilterNode,
) : FilterNode()

data class ValuePathExpression(
    val attributePath: String,
    val filter: FilterNode,
    val subAttribute: String? = null,
) : FilterNode()
