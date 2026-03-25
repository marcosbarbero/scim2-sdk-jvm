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
package com.marcosbarbero.scim2.core.validation

import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.ScimAttribute
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class ScimValidator {

    fun validateForCreate(resource: ScimResource): List<String> {
        val errors = mutableListOf<String>()

        for (prop in resource::class.memberProperties) {
            val attr = prop.findAnnotation<ScimAttribute>()

            // Check required attributes
            if (attr?.required == true) {
                val value = prop.getter.call(resource)
                if (value == null || (value is String && value.isBlank())) {
                    errors.add("Required attribute '${prop.name}' must not be null or blank")
                }
            }

            // Flag readOnly attributes set by client
            if (attr?.mutability == Mutability.READ_ONLY) {
                val value = prop.getter.call(resource)
                if (value != null) {
                    errors.add("Attribute '${prop.name}' is readOnly and should not be set by client")
                }
            }
        }

        // Also check common readOnly fields not annotated
        resource.id?.let {
            errors.add("Attribute 'id' is readOnly and should not be set by client")
        }

        return errors
    }

    fun validateForReplace(existing: ScimResource, replacement: ScimResource): List<String> {
        val errors = mutableListOf<String>()

        for (prop in replacement::class.memberProperties) {
            val attr = prop.findAnnotation<ScimAttribute>()

            if (attr?.required == true) {
                val value = prop.getter.call(replacement)
                if (value == null || (value is String && value.isBlank())) {
                    errors.add("Required attribute '${prop.name}' must not be null or blank in replacement")
                }
            }
        }

        return errors
    }

    fun validatePatch(request: PatchRequest): List<String> {
        val errors = mutableListOf<String>()

        if (request.operations.isEmpty()) {
            errors.add("PatchRequest must contain at least one operation")
        }

        for ((index, op) in request.operations.withIndex()) {
            if (op.op == PatchOp.REMOVE && op.path.isNullOrBlank()) {
                errors.add("REMOVE operation at index $index requires a path")
            }
        }

        return errors
    }
}
