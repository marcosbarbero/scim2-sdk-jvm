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
package com.marcosbarbero.scim2.core.serialization.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.KotlinModule
import kotlin.reflect.KClass

class JacksonScimSerializer(private val objectMapper: ObjectMapper) : ScimSerializer {

    constructor() : this(defaultObjectMapper())

    override fun <T : Any> serialize(value: T): ByteArray = objectMapper.writeValueAsBytes(value)

    override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T = objectMapper.readValue(bytes, type.java)

    override fun serializeToString(value: Any): String = objectMapper.writeValueAsString(value)

    override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T = objectMapper.readValue(json, type.java)

    override fun enrichMetaLocation(json: ByteArray, location: String, resourceType: String?): ByteArray {
        val tree = objectMapper.readTree(json) as ObjectNode
        val metaNode = tree.get("meta")
        val meta = if (metaNode != null && metaNode is ObjectNode) {
            metaNode
        } else {
            objectMapper.createObjectNode().also { tree.set("meta", it) }
        }
        meta.put("location", location)
        if (resourceType != null && !meta.has("resourceType")) {
            meta.put("resourceType", resourceType)
        }
        return objectMapper.writeValueAsBytes(tree)
    }

    override fun enrichMemberRefs(json: ByteArray, baseScimUrl: String): ByteArray {
        val tree = objectMapper.readTree(json) as ObjectNode
        val base = baseScimUrl.trimEnd('/')
        enrichArrayRefs(tree, "members", base)
        enrichArrayRefs(tree, "groups", base)
        return objectMapper.writeValueAsBytes(tree)
    }

    private fun enrichArrayRefs(tree: ObjectNode, fieldName: String, baseScimUrl: String) {
        val array = tree.get(fieldName) ?: return
        if (!array.isArray) return
        for (element in array) {
            if (element !is ObjectNode) continue
            if (element.has("\$ref")) continue
            val value = element.get("value")?.asString() ?: continue
            val type = element.get("type")?.asString() ?: continue
            element.put("\$ref", "$baseScimUrl/${type}s/$value")
        }
    }

    companion object {
        fun defaultObjectMapper(): ObjectMapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .addModule(ScimModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion { incl ->
                incl.withValueInclusion(JsonInclude.Include.NON_NULL)
            }
            .build()
    }
}
