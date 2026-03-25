package com.marcosbarbero.scim2.core.serialization.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import kotlin.reflect.KClass

class JacksonScimSerializer(private val objectMapper: ObjectMapper) : ScimSerializer {

    constructor() : this(defaultObjectMapper())

    override fun <T : Any> serialize(value: T): ByteArray {
        return objectMapper.writeValueAsBytes(value)
    }

    override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T {
        return objectMapper.readValue(bytes, type.java)
    }

    override fun serializeToString(value: Any): String {
        return objectMapper.writeValueAsString(value)
    }

    override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T {
        return objectMapper.readValue(json, type.java)
    }

    companion object {
        fun defaultObjectMapper(): ObjectMapper {
            return JsonMapper.builder()
                .addModule(KotlinModule.Builder().build())
                .addModule(ScimModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultPropertyInclusion { incl ->
                    incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                }
                .build()
        }
    }
}
