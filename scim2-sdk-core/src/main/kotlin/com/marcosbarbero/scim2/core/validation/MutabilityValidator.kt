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
package com.marcosbarbero.scim2.core.validation

import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.ScimAttribute
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Validates whether an attribute path is allowed to be modified on a given resource class
 * based on the `@ScimAttribute(mutability = ...)` annotation.
 *
 * Per RFC 7643 §7:
 * - READ_ONLY attributes cannot be modified (ever)
 * - IMMUTABLE attributes can only be set on creation (not via PATCH/PUT)
 */
object MutabilityValidator {

    private val mutabilityCache = ConcurrentHashMap<KClass<*>, Map<String, Mutability>>()

    /**
     * Returns `true` if the attribute at the given [path] is allowed to be modified
     * via PATCH or PUT on the given [resourceClass].
     *
     * Attributes without a `@ScimAttribute` annotation are assumed to be READ_WRITE.
     * Filter paths (e.g., `emails[type eq "work"]`) use only the base attribute name.
     */
    @JvmStatic
    fun isModificationAllowed(resourceClass: KClass<*>, path: String): Boolean {
        val basePath = extractBasePath(path)
        val mutabilityMap = resolveMutabilityMap(resourceClass)
        val mutability = mutabilityMap[basePath.lowercase()] ?: return true
        return mutability != Mutability.READ_ONLY && mutability != Mutability.IMMUTABLE
    }

    private fun resolveMutabilityMap(klass: KClass<*>): Map<String, Mutability> = mutabilityCache.getOrPut(klass) {
        val result = mutableMapOf<String, Mutability>()
        // Check all member properties (includes inherited) via Kotlin reflection
        klass.memberProperties.forEach { prop ->
            val annotation = findScimAttribute(prop)
            if (annotation != null) {
                result.putIfAbsent(prop.name.lowercase(), annotation.mutability)
            }
        }
        // Walk the class hierarchy for constructor parameter annotations on superclasses
        collectSuperclassAnnotations(klass, result)
        result
    }

    private fun findScimAttribute(prop: kotlin.reflect.KProperty<*>): ScimAttribute? = prop.javaField?.getAnnotation(ScimAttribute::class.java)
        ?: prop.annotations.filterIsInstance<ScimAttribute>().firstOrNull()
        ?: prop.getter.annotations.filterIsInstance<ScimAttribute>().firstOrNull()

    private fun collectSuperclassAnnotations(klass: KClass<*>, result: MutableMap<String, Mutability>) {
        val supertypes = klass.supertypes
        for (supertype in supertypes) {
            val superClass = supertype.classifier as? KClass<*> ?: continue
            if (superClass == Any::class) continue
            // Check declared properties of the superclass directly
            superClass.declaredMemberProperties.forEach { prop ->
                val annotation = findScimAttribute(prop)
                if (annotation != null) {
                    result.putIfAbsent(prop.name.lowercase(), annotation.mutability)
                }
            }
            // Recurse
            collectSuperclassAnnotations(superClass, result)
        }
    }

    private fun extractBasePath(path: String): String {
        val bracketIndex = path.indexOf('[')
        return if (bracketIndex > 0) path.substring(0, bracketIndex) else path
    }
}
