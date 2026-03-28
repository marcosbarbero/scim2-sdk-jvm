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
package com.marcosbarbero.scim2.core.serialization.spi

import kotlin.reflect.KClass

interface ScimSerializer {

    fun <T : Any> serialize(value: T): ByteArray

    fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T

    fun serializeToString(value: Any): String

    fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T

    /**
     * Enriches already-serialized JSON bytes by setting `meta.location` (and optionally
     * `meta.resourceType` if not already present).
     *
     * Operates on the wire format to avoid lossy domain-object round-trips.
     */
    fun enrichMetaLocation(json: ByteArray, location: String, resourceType: String? = null): ByteArray
}
