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
