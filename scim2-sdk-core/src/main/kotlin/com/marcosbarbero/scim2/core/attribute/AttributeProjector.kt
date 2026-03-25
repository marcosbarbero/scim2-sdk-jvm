package com.marcosbarbero.scim2.core.attribute

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource

class AttributeProjector(private val objectMapper: ObjectMapper) {

    fun <T : ScimResource> project(
        resource: T,
        attributes: List<String>? = null,
        excludedAttributes: List<String>? = null
    ): T {
        if (!attributes.isNullOrEmpty() && !excludedAttributes.isNullOrEmpty()) {
            throw InvalidValueException("Cannot specify both 'attributes' and 'excludedAttributes'")
        }

        if (attributes.isNullOrEmpty() && excludedAttributes.isNullOrEmpty()) {
            return resource
        }

        val node = objectMapper.valueToTree<ObjectNode>(resource)

        if (!attributes.isNullOrEmpty()) {
            applyInclude(node, attributes)
        } else if (!excludedAttributes.isNullOrEmpty()) {
            applyExclude(node, excludedAttributes)
        }

        @Suppress("UNCHECKED_CAST")
        return objectMapper.treeToValue(node, resource::class.java) as T
    }

    private fun applyInclude(node: ObjectNode, attributes: List<String>) {
        val include = attributes.map { it.lowercase() }.toSet() + ALWAYS_RETURNED
        val fieldsToRemove = mutableListOf<String>()
        node.fieldNames().forEach { field ->
            if (field.lowercase() !in include) {
                fieldsToRemove.add(field)
            }
        }
        fieldsToRemove.forEach { node.remove(it) }
    }

    private fun applyExclude(node: ObjectNode, excludedAttributes: List<String>) {
        val exclude = excludedAttributes.map { it.lowercase() }.toSet()
        val fieldsToRemove = mutableListOf<String>()
        node.fieldNames().forEach { field ->
            if (field.lowercase() in exclude && field.lowercase() !in ALWAYS_RETURNED) {
                fieldsToRemove.add(field)
            }
        }
        fieldsToRemove.forEach { node.remove(it) }
    }

    companion object {
        private val ALWAYS_RETURNED = setOf("id", "schemas")
    }
}
