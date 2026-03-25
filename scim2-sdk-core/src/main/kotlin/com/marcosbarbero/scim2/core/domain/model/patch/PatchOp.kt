package com.marcosbarbero.scim2.core.domain.model.patch

import com.fasterxml.jackson.annotation.JsonValue

enum class PatchOp(@get:JsonValue val value: String) {
    ADD("add"),
    REMOVE("remove"),
    REPLACE("replace")
}
