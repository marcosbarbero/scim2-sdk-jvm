package com.marcosbarbero.scim2.core.schema.introspector

import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.Schema
import com.marcosbarbero.scim2.core.schema.annotation.ScimExtension
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class SchemaRegistry {

    private val introspector = SchemaIntrospector()
    private val schemas = ConcurrentHashMap<String, Schema>()
    private val resourceTypes = ConcurrentHashMap<String, ResourceType>()

    fun register(type: KClass<out ScimResource>) {
        val schema = introspector.introspect(type)
        schemas[schema.id] = schema

        val resourceType = introspector.introspectResourceType(type)
        resourceTypes[resourceType.name] = resourceType
    }

    fun registerExtension(resourceType: KClass<out ScimResource>, extensionType: KClass<*>) {
        val extensionAnnotation = extensionType.findAnnotation<ScimExtension>()
            ?: throw IllegalArgumentException(
                "Class ${extensionType.simpleName} must be annotated with @ScimExtension"
            )

        val extensionSchema = introspector.introspect(extensionType)
        schemas[extensionSchema.id] = extensionSchema

        val rtAnnotation = resourceType.findAnnotation<com.marcosbarbero.scim2.core.schema.annotation.ScimResource>()
            ?: throw IllegalArgumentException(
                "Class ${resourceType.simpleName} must be annotated with @ScimResource"
            )

        val extension = ResourceType.SchemaExtension(
            schema = extensionAnnotation.schema,
            required = false
        )

        resourceTypes.compute(rtAnnotation.name) { _, existing ->
            existing
                ?: throw IllegalStateException(
                    "Resource type ${rtAnnotation.name} must be registered before adding extensions"
                )
            existing.copy(schemaExtensions = existing.schemaExtensions + extension)
        }
    }

    fun getSchema(uri: String): Schema? = schemas[uri]

    fun getResourceType(name: String): ResourceType? = resourceTypes[name]

    fun getAllSchemas(): List<Schema> = schemas.values.toList()

    fun getAllResourceTypes(): List<ResourceType> = resourceTypes.values.toList()
}
