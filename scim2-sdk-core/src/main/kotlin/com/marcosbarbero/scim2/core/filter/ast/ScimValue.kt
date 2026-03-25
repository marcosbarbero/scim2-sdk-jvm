package com.marcosbarbero.scim2.core.filter.ast

sealed class ScimValue {
    data class StringValue(val value: String) : ScimValue()
    data class NumberValue(val value: Number) : ScimValue()
    data class BooleanValue(val value: Boolean) : ScimValue()
    data object NullValue : ScimValue()
}
