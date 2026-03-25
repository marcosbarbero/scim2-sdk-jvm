package com.marcosbarbero.scim2.core.domain.vo

@JvmInline
value class SchemaUri(val value: String) {
    init {
        require(value.isNotBlank()) { "SchemaUri must not be blank" }
        require(value.startsWith("urn:")) { "SchemaUri must start with 'urn:'" }
    }

    override fun toString(): String = value
}
