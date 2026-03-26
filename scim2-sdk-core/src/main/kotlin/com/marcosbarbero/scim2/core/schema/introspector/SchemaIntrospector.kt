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
package com.marcosbarbero.scim2.core.schema.introspector

import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.Schema
import com.marcosbarbero.scim2.core.domain.model.schema.SchemaAttribute
import com.marcosbarbero.scim2.core.schema.annotation.AttributeType
import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.Returned
import com.marcosbarbero.scim2.core.schema.annotation.ScimAttribute
import com.marcosbarbero.scim2.core.schema.annotation.ScimExtension
import com.marcosbarbero.scim2.core.schema.annotation.ScimResource
import com.marcosbarbero.scim2.core.schema.annotation.Uniqueness
import java.net.URI
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

class SchemaIntrospector {

    private val baseResourceProperties = setOf("schemas", "id", "externalId", "meta", "extensions")

    fun introspect(type: KClass<*>): Schema {
        val resourceAnnotation = type.findAnnotation<ScimResource>()
        val extensionAnnotation = type.findAnnotation<ScimExtension>()

        val id = resourceAnnotation?.schema
            ?: extensionAnnotation?.schema
            ?: throw IllegalArgumentException(
                "Class ${type.simpleName} must be annotated with @ScimResource or @ScimExtension",
            )

        val name = resourceAnnotation?.name
        val description = resourceAnnotation?.description?.takeIf { it.isNotEmpty() }

        val attributes = introspectAttributes(type)

        return Schema(
            id = id,
            name = name,
            description = description,
            attributes = attributes,
        )
    }

    fun introspectResourceType(type: KClass<*>): ResourceType {
        val annotation = type.findAnnotation<ScimResource>()
            ?: throw IllegalArgumentException(
                "Class ${type.simpleName} must be annotated with @ScimResource",
            )

        return ResourceType(
            id = annotation.name,
            name = annotation.name,
            description = annotation.description.takeIf { it.isNotEmpty() },
            endpoint = annotation.endpoint,
            schema = annotation.schema,
        )
    }

    private fun introspectAttributes(type: KClass<*>): List<SchemaAttribute> = type.declaredMemberProperties
        .filter { it.name !in baseResourceProperties }
        .map { property -> introspectProperty(property) }

    private fun introspectProperty(property: KProperty1<*, *>): SchemaAttribute {
        val annotation = property.findAnnotation<ScimAttribute>()
        val propertyType = property.returnType.jvmErasure
        val isNullable = property.returnType.isMarkedNullable
        val isList = propertyType == List::class

        val (attrType, subAttributes) = resolveType(property, isList)
        val isMultiValued = annotation?.multiValued ?: isList

        return SchemaAttribute(
            name = annotation?.name?.takeIf { it.isNotEmpty() } ?: property.name,
            type = annotation?.type ?: attrType,
            multiValued = isMultiValued,
            description = annotation?.description ?: "",
            required = annotation?.required ?: false,
            caseExact = annotation?.caseExact ?: false,
            mutability = annotation?.mutability ?: Mutability.READ_WRITE,
            returned = annotation?.returned ?: Returned.DEFAULT,
            uniqueness = annotation?.uniqueness ?: Uniqueness.NONE,
            subAttributes = subAttributes,
        )
    }

    private fun resolveType(
        property: KProperty1<*, *>,
        isList: Boolean,
    ): Pair<AttributeType, List<SchemaAttribute>> {
        val rawType = if (isList) {
            property.returnType.arguments.firstOrNull()?.type?.jvmErasure ?: Any::class
        } else {
            property.returnType.jvmErasure
        }

        return when {
            rawType == String::class -> AttributeType.STRING to emptyList()
            rawType == Boolean::class -> AttributeType.BOOLEAN to emptyList()
            rawType == Int::class || rawType == Long::class -> AttributeType.INTEGER to emptyList()
            rawType == Double::class || rawType == Float::class -> AttributeType.DECIMAL to emptyList()
            rawType == Instant::class -> AttributeType.DATE_TIME to emptyList()
            rawType == URI::class -> AttributeType.REFERENCE to emptyList()
            rawType == ByteArray::class -> AttributeType.BINARY to emptyList()
            isComplexType(rawType) -> {
                val subAttrs = rawType.declaredMemberProperties.map { introspectProperty(it) }
                AttributeType.COMPLEX to subAttrs
            }
            else -> AttributeType.STRING to emptyList()
        }
    }

    private fun isComplexType(type: KClass<*>): Boolean = type.isData && type != String::class
}
