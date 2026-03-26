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
import tools.jackson.module.kotlin.KotlinModule
import kotlin.reflect.KClass

class JacksonScimSerializer(private val objectMapper: ObjectMapper) : ScimSerializer {

    constructor() : this(defaultObjectMapper())

    override fun <T : Any> serialize(value: T): ByteArray = objectMapper.writeValueAsBytes(value)

    override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T = objectMapper.readValue(bytes, type.java)

    override fun serializeToString(value: Any): String = objectMapper.writeValueAsString(value)

    override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T = objectMapper.readValue(json, type.java)

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
