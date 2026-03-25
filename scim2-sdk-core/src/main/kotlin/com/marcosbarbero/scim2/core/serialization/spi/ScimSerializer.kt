package com.marcosbarbero.scim2.core.serialization.spi

import kotlin.reflect.KClass

interface ScimSerializer {

    fun <T : Any> serialize(value: T): ByteArray

    fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T

    fun serializeToString(value: Any): String

    fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T
}
