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
import com.marcosbarbero.scim2.core.domain.model.common.Address
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.common.Name
import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.Returned
import com.marcosbarbero.scim2.core.schema.annotation.ScimAttribute
import com.marcosbarbero.scim2.core.schema.annotation.Uniqueness
import java.net.URI

@com.marcosbarbero.scim2.core.schema.annotation.ScimResource(
    schema = ScimUrns.USER,
    name = "User",
    description = "User Account",
    endpoint = "/Users",
)
data class User(
    override val id: String? = null,
    override val externalId: String? = null,
    override val meta: Meta? = null,

    @ScimAttribute(required = true, uniqueness = Uniqueness.SERVER)
    val userName: String,

    val name: Name? = null,
    val displayName: String? = null,
    val nickName: String? = null,
    val profileUrl: URI? = null,
    val title: String? = null,
    val userType: String? = null,
    val preferredLanguage: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
    val active: Boolean? = null,

    @ScimAttribute(mutability = Mutability.WRITE_ONLY, returned = Returned.NEVER)
    val password: String? = null,

    val emails: List<MultiValuedAttribute> = emptyList(),
    val phoneNumbers: List<MultiValuedAttribute> = emptyList(),
    val ims: List<MultiValuedAttribute> = emptyList(),
    val photos: List<MultiValuedAttribute> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val groups: List<GroupMembership> = emptyList(),
    val entitlements: List<MultiValuedAttribute> = emptyList(),
    val roles: List<MultiValuedAttribute> = emptyList(),
    val x509Certificates: List<MultiValuedAttribute> = emptyList(),
) : ScimResource(
    schemas = listOf(ScimUrns.USER),
    id = id,
    externalId = externalId,
    meta = meta,
)
