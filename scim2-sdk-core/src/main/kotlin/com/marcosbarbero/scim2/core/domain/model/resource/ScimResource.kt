package com.marcosbarbero.scim2.core.domain.model.resource

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.marcosbarbero.scim2.core.domain.model.common.Meta

abstract class ScimResource(
    open val schemas: List<String>,
    open val id: String? = null,
    open val externalId: String? = null,
    open val meta: Meta? = null
) {
    private val _extensions: MutableMap<String, Any> = mutableMapOf()

    @get:JsonAnyGetter
    val extensions: Map<String, Any> get() = _extensions

    @JsonAnySetter
    fun setExtension(key: String, value: Any) {
        _extensions[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getExtension(schema: String): T? = _extensions[schema] as? T
}
