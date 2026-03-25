package com.marcosbarbero.scim2.core.domain.model.common

import com.marcosbarbero.scim2.core.domain.vo.ETag
import java.net.URI
import java.time.Instant

data class Meta(
    val resourceType: String? = null,
    val created: Instant? = null,
    val lastModified: Instant? = null,
    val location: URI? = null,
    val version: ETag? = null
)
