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
