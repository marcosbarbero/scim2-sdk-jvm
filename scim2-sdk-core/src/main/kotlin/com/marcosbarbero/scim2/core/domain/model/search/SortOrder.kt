package com.marcosbarbero.scim2.core.domain.model.search

import com.fasterxml.jackson.annotation.JsonValue

enum class SortOrder(@get:JsonValue val value: String) {
    ASCENDING("ascending"),
    DESCENDING("descending")
}
