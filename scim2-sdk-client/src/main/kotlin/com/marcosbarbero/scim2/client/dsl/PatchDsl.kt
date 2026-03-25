package com.marcosbarbero.scim2.client.dsl

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest

fun scimPatch(block: PatchBuilder.() -> Unit): PatchRequest {
    val builder = PatchBuilder()
    builder.block()
    return builder.build()
}

class PatchBuilder {
    private val operations = mutableListOf<PatchOperation>()

    fun add(path: String? = null, value: JsonNode) {
        operations.add(PatchOperation(PatchOp.ADD, path, value))
    }

    fun remove(path: String) {
        operations.add(PatchOperation(PatchOp.REMOVE, path, null))
    }

    fun replace(path: String? = null, value: JsonNode) {
        operations.add(PatchOperation(PatchOp.REPLACE, path, value))
    }

    fun add(path: String, value: String) {
        add(path, StringNode(value))
    }

    fun replace(path: String, value: String) {
        replace(path, StringNode(value))
    }

    fun build() = PatchRequest(operations = operations.toList())
}
