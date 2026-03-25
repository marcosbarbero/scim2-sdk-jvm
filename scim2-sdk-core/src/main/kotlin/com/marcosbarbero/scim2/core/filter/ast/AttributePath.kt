package com.marcosbarbero.scim2.core.filter.ast

data class AttributePath(
    val schemaUri: String? = null,
    val attributeName: String,
    val subAttribute: String? = null
) {
    fun toFullPath(): String = buildString {
        if (schemaUri != null) {
            append(schemaUri)
            append(":")
        }
        append(attributeName)
        if (subAttribute != null) {
            append(".")
            append(subAttribute)
        }
    }
}
