/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
