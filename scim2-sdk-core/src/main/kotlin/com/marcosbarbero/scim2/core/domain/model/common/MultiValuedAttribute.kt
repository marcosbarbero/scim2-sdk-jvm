package com.marcosbarbero.scim2.core.domain.model.common

import com.fasterxml.jackson.annotation.JsonProperty

data class MultiValuedAttribute(
    val value: String? = null,
    val display: String? = null,
    val type: String? = null,
    val primary: Boolean? = null,
    @JsonProperty("\$ref")
    val ref: String? = null
)
