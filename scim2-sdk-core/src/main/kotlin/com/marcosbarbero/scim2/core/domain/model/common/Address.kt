package com.marcosbarbero.scim2.core.domain.model.common

data class Address(
    val formatted: String? = null,
    val streetAddress: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val type: String? = null,
    val primary: Boolean? = null
)
