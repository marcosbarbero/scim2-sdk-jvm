package com.marcosbarbero.scim2.core.domain.model.schema

data class ResourceType(
    val id: String,
    val name: String,
    val description: String?,
    val endpoint: String,
    val schema: String,
    val schemaExtensions: List<SchemaExtension> = emptyList()
) {
    data class SchemaExtension(val schema: String, val required: Boolean)
}
