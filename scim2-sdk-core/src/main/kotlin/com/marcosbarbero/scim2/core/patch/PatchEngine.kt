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
package com.marcosbarbero.scim2.core.patch

import com.marcosbarbero.scim2.core.domain.model.error.InvalidPathException
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

class PatchEngine(private val objectMapper: ObjectMapper) {

    fun <T : ScimResource> apply(resource: T, request: PatchRequest): T {
        var node = objectMapper.valueToTree<ObjectNode>(resource)

        for (operation in request.operations) {
            node = applyOperation(node, operation)
        }

        @Suppress("UNCHECKED_CAST")
        return objectMapper.treeToValue(node, resource::class.java) as T
    }

    private fun applyOperation(node: ObjectNode, operation: PatchOperation): ObjectNode = when (operation.op) {
        PatchOp.ADD -> applyAdd(node, operation)
        PatchOp.REMOVE -> applyRemove(node, operation)
        PatchOp.REPLACE -> applyReplace(node, operation)
    }

    private fun applyAdd(node: ObjectNode, operation: PatchOperation): ObjectNode {
        val path = operation.path
        val value = operation.value

        path ?: run {
            // No path: value must be an object, merge its attributes into the resource
            (value as? ObjectNode)?.propertyNames()?.forEach { fieldName ->
                node.set(fieldName, value.get(fieldName))
            } ?: throw InvalidValueException("ADD operation with no path requires an object value")
            return node
        }

        // Check for value path filter (e.g., emails[type eq "work"])
        FILTER_PATH_REGEX.matchEntire(path)?.let { filterMatch ->
            val attrName = filterMatch.groupValues[1]
            val filterAttr = filterMatch.groupValues[2]
            val filterValue = filterMatch.groupValues[3]
            addToFilteredMultiValued(node, attrName, filterAttr, filterValue, value)
            return node
        }

        // Simple path
        val existing = node.get(path)
        if (existing != null && existing.isArray) {
            // Append to existing array
            val array = existing as ArrayNode
            value?.takeIf { it.isArray }?.forEach { array.add(it) } ?: array.add(value)
        } else {
            node.set(path, value)
        }
        return node
    }

    private fun applyRemove(node: ObjectNode, operation: PatchOperation): ObjectNode {
        val path = operation.path
            ?: throw InvalidPathException("REMOVE operation requires a path")

        FILTER_PATH_REGEX.matchEntire(path)?.let { filterMatch ->
            val attrName = filterMatch.groupValues[1]
            val filterAttr = filterMatch.groupValues[2]
            val filterValue = filterMatch.groupValues[3]
            removeFromMultiValued(node, attrName, filterAttr, filterValue)
            return node
        }

        node.remove(path)
        return node
    }

    private fun applyReplace(node: ObjectNode, operation: PatchOperation): ObjectNode {
        val path = operation.path
        val value = operation.value

        path ?: run {
            // No path: value must be an object, replace its attributes
            (value as? ObjectNode)?.propertyNames()?.forEach { fieldName ->
                node.set(fieldName, value.get(fieldName))
            } ?: throw InvalidValueException("REPLACE operation with no path requires an object value")
            return node
        }

        node.set(path, value)
        return node
    }

    private fun addToFilteredMultiValued(
        node: ObjectNode,
        attrName: String,
        filterAttr: String,
        filterValue: String,
        value: JsonNode?,
    ) {
        val array = node.get(attrName) as? ArrayNode ?: return
        for (i in 0 until array.size()) {
            val element = array.get(i) as? ObjectNode ?: continue
            val fieldValue = element.get(filterAttr)?.asText()
            if (fieldValue.equals(filterValue, ignoreCase = true)) {
                (value as? ObjectNode)?.propertyNames()?.forEach { field ->
                    element.set(field, value.get(field))
                } ?: throw InvalidValueException("Filtered multi-valued attribute update requires an object value")
            }
        }
    }

    private fun removeFromMultiValued(
        node: ObjectNode,
        attrName: String,
        filterAttr: String,
        filterValue: String,
    ) {
        val array = node.get(attrName) as? ArrayNode ?: return
        val newArray = objectMapper.createArrayNode()
        for (i in 0 until array.size()) {
            val element = array.get(i) as? ObjectNode
            val fieldValue = element?.get(filterAttr)?.asText()
            if (!fieldValue.equals(filterValue, ignoreCase = true)) {
                newArray.add(array.get(i))
            }
        }
        node.set(attrName, newArray)
    }

    companion object {
        private val FILTER_PATH_REGEX = Regex("""(\w+)\[(\w+)\s+eq\s+"([^"]+)"]""")
    }
}
