package com.marcosbarbero.scim2.core.domain.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class GroupMembership(
    val value: String? = null,
    @JsonProperty("\$ref")
    val ref: URI? = null,
    val display: String? = null,
    val type: String? = null
)
