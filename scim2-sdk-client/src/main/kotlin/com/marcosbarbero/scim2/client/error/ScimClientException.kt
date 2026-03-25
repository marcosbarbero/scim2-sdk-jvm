package com.marcosbarbero.scim2.client.error

import com.marcosbarbero.scim2.core.domain.model.error.ScimError

class ScimClientException(
    val statusCode: Int,
    val scimError: ScimError? = null,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message ?: scimError?.detail ?: "SCIM client error ($statusCode)", cause)
