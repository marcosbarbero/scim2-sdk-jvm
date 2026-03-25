package com.marcosbarbero.scim2.core.domain.vo

@JvmInline
value class ResourceId(val value: String) {
    init {
        require(value.isNotBlank()) { "ResourceId must not be blank" }
    }

    override fun toString(): String = value
}
