package com.marcosbarbero.scim2.core.domain.model.resource

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.common.Manager
import com.marcosbarbero.scim2.core.schema.annotation.ScimExtension

@ScimExtension(schema = ScimUrns.ENTERPRISE_USER)
data class EnterpriseUserExtension(
    val employeeNumber: String? = null,
    val costCenter: String? = null,
    val organization: String? = null,
    val division: String? = null,
    val department: String? = null,
    val manager: Manager? = null
)
