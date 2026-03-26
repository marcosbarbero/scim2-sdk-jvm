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
package com.marcosbarbero.scim2.core.attribute

import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

class AttributeProjector(private val objectMapper: ObjectMapper) {

    fun <T : ScimResource> project(
        resource: T,
        attributes: List<String>? = null,
        excludedAttributes: List<String>? = null,
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
        node.propertyNames().forEach { field ->
            if (field.lowercase() !in include) {
                fieldsToRemove.add(field)
            }
        }
        fieldsToRemove.forEach { node.remove(it) }
    }

    private fun applyExclude(node: ObjectNode, excludedAttributes: List<String>) {
        val exclude = excludedAttributes.map { it.lowercase() }.toSet()
        val fieldsToRemove = mutableListOf<String>()
        node.propertyNames().forEach { field ->
            if (field.lowercase() in exclude && field.lowercase() !in ALWAYS_RETURNED) {
                fieldsToRemove.add(field)
            }
        }
        fieldsToRemove.forEach { node.remove(it) }
    }

    companion object {
        private val ALWAYS_RETURNED = setOf("id", "schemas", "meta")
    }
}
