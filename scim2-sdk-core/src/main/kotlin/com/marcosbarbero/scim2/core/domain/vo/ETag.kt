package com.marcosbarbero.scim2.core.domain.vo

@JvmInline
value class ETag(val value: String) {
    override fun toString(): String = value
}
