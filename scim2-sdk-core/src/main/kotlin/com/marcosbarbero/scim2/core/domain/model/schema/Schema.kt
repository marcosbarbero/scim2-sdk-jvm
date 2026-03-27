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
package com.marcosbarbero.scim2.core.domain.model.schema

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.schema.annotation.AttributeType
import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.Returned
import com.marcosbarbero.scim2.core.schema.annotation.Uniqueness

data class Schema(
    val schemas: List<String> = listOf(ScimUrns.SCHEMA),
    val id: String,
    val name: String?,
    val description: String?,
    val attributes: List<SchemaAttribute>,
)

data class SchemaAttribute(
    val name: String,
    val type: AttributeType,
    val multiValued: Boolean = false,
    val description: String = "",
    val required: Boolean = false,
    val canonicalValues: List<String> = emptyList(),
    val caseExact: Boolean = false,
    val mutability: Mutability = Mutability.READ_WRITE,
    val returned: Returned = Returned.DEFAULT,
    val uniqueness: Uniqueness = Uniqueness.NONE,
    val referenceTypes: List<String> = emptyList(),
    val subAttributes: List<SchemaAttribute> = emptyList(),
)
