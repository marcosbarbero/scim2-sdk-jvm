package com.marcosbarbero.scim2.core.domain.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class Manager(
    val value: String? = null,
    @JsonProperty("\$ref")
    val ref: URI? = null,
    val displayName: String? = null
)
