package com.marcosbarbero.scim2.core.domain.model.schema

import com.marcosbarbero.scim2.core.schema.annotation.AttributeType
import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.Returned
import com.marcosbarbero.scim2.core.schema.annotation.Uniqueness

data class Schema(
    val id: String,
    val name: String?,
    val description: String?,
    val attributes: List<SchemaAttribute>
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
    val subAttributes: List<SchemaAttribute> = emptyList()
)
