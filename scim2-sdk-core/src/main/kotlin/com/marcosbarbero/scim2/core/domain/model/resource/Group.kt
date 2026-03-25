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
package com.marcosbarbero.scim2.core.domain.model.resource

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.schema.annotation.ScimAttribute

@com.marcosbarbero.scim2.core.schema.annotation.ScimResource(
    schema = ScimUrns.GROUP,
    name = "Group",
    description = "Group",
    endpoint = "/Groups"
)
data class Group(
    override val id: String? = null,
    override val externalId: String? = null,
    override val meta: Meta? = null,

    @ScimAttribute(required = true)
    val displayName: String,

    val members: List<GroupMembership> = emptyList()
) : ScimResource(
    schemas = listOf(ScimUrns.GROUP),
    id = id,
    externalId = externalId,
    meta = meta
)
