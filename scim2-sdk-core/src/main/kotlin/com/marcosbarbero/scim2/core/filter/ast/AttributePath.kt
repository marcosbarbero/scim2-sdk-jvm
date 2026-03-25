package com.marcosbarbero.scim2.core.filter.ast

data class AttributePath(
    val schemaUri: String? = null,
    val attributeName: String,
    val subAttribute: String? = null
) {
    fun toFullPath(): String = buildString {
        schemaUri?.let {
            append(it)
            append(":")
        }
        append(attributeName)
        subAttribute?.let {
            append(".")
            append(it)
        }
    }
}
