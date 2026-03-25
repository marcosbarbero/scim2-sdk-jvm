package com.marcosbarbero.scim2.core.patch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.marcosbarbero.scim2.core.domain.model.error.InvalidPathException
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource

class PatchEngine(private val objectMapper: ObjectMapper) {

    fun <T : ScimResource> apply(resource: T, request: PatchRequest): T {
        var node = objectMapper.valueToTree<ObjectNode>(resource)

        for (operation in request.operations) {
            node = applyOperation(node, operation)
        }

        @Suppress("UNCHECKED_CAST")
        return objectMapper.treeToValue(node, resource::class.java) as T
    }

    private fun applyOperation(node: ObjectNode, operation: PatchOperation): ObjectNode {
        return when (operation.op) {
            PatchOp.ADD -> applyAdd(node, operation)
            PatchOp.REMOVE -> applyRemove(node, operation)
            PatchOp.REPLACE -> applyReplace(node, operation)
        }
    }

    private fun applyAdd(node: ObjectNode, operation: PatchOperation): ObjectNode {
        val path = operation.path
        val value = operation.value

        if (path == null) {
            // No path: value must be an object, merge its attributes into the resource
            if (value is Map<*, *>) {
                val valueNode = objectMapper.valueToTree<ObjectNode>(value)
                val fieldNames = valueNode.fieldNames()
                while (fieldNames.hasNext()) {
                    val fieldName = fieldNames.next()
                    node.set<com.fasterxml.jackson.databind.JsonNode>(fieldName, valueNode.get(fieldName))
                }
            } else {
                throw InvalidValueException("ADD operation with no path requires an object value")
            }
            return node
        }

        // Check for value path filter (e.g., emails[type eq "work"])
        val filterMatch = FILTER_PATH_REGEX.matchEntire(path)
        if (filterMatch != null) {
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
            val valueNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(value)
            if (valueNode.isArray) {
                valueNode.forEach { array.add(it) }
            } else {
                array.add(valueNode)
            }
        } else {
            node.set<com.fasterxml.jackson.databind.JsonNode>(path, objectMapper.valueToTree(value))
        }
        return node
    }

    private fun applyRemove(node: ObjectNode, operation: PatchOperation): ObjectNode {
        val path = operation.path
            ?: throw InvalidPathException("REMOVE operation requires a path")

        val filterMatch = FILTER_PATH_REGEX.matchEntire(path)
        if (filterMatch != null) {
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

        if (path == null) {
            // No path: value must be an object, replace its attributes
            if (value is Map<*, *>) {
                val valueNode = objectMapper.valueToTree<ObjectNode>(value)
                val fieldNames = valueNode.fieldNames()
                while (fieldNames.hasNext()) {
                    val fieldName = fieldNames.next()
                    node.set<com.fasterxml.jackson.databind.JsonNode>(fieldName, valueNode.get(fieldName))
                }
            }
            return node
        }

        node.set<com.fasterxml.jackson.databind.JsonNode>(path, objectMapper.valueToTree(value))
        return node
    }

    private fun addToFilteredMultiValued(
        node: ObjectNode,
        attrName: String,
        filterAttr: String,
        filterValue: String,
        value: Any?
    ) {
        val array = node.get(attrName) as? ArrayNode ?: return
        for (i in 0 until array.size()) {
            val element = array.get(i) as? ObjectNode ?: continue
            val fieldValue = element.get(filterAttr)?.asText()
            if (fieldValue.equals(filterValue, ignoreCase = true)) {
                if (value is Map<*, *>) {
                    val valueNode = objectMapper.valueToTree<ObjectNode>(value)
                    valueNode.fieldNames().forEach { field ->
                        element.set<com.fasterxml.jackson.databind.JsonNode>(field, valueNode.get(field))
                    }
                }
            }
        }
    }

    private fun removeFromMultiValued(
        node: ObjectNode,
        attrName: String,
        filterAttr: String,
        filterValue: String
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
        node.set<com.fasterxml.jackson.databind.JsonNode>(attrName, newArray)
    }

    companion object {
        private val FILTER_PATH_REGEX = Regex("""(\w+)\[(\w+)\s+eq\s+"([^"]+)"]""")
    }
}
