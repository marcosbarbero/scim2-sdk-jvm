package com.marcosbarbero.scim2.core.serialization.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
            return ObjectMapper().apply {
                registerModule(KotlinModule.Builder().build())
                registerModule(JavaTimeModule())
                registerModule(ScimModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
    }
}
