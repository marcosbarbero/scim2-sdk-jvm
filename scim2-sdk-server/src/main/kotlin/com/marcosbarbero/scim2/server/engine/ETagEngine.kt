package com.marcosbarbero.scim2.server.engine

import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.server.http.ScimHttpRequest

class ETagEngine {

    fun generateETag(resource: ScimResource): ETag {
        val version = resource.meta?.version
        if (version != null) return version

        val hash = resource.hashCode().toUInt().toString(16)
        return ETag("W/\"$hash\"")
    }

    fun extractIfMatch(request: ScimHttpRequest): ETag? {
        val header = request.header("If-Match") ?: return null
        return ETag(header)
    }

    fun extractIfNoneMatch(request: ScimHttpRequest): ETag? {
        val header = request.header("If-None-Match") ?: return null
        return ETag(header)
    }

    fun checkPrecondition(current: ETag?, required: ETag?): Boolean {
        if (required == null) return true
        if (current == null) return true
        return current == required
    }

    fun checkPreconditionOrThrow(current: ETag?, required: ETag?) {
        if (!checkPrecondition(current, required)) {
            throw PreconditionFailedException(
                "ETag mismatch: expected ${required?.value}, got ${current?.value}"
            )
        }
    }
}

class PreconditionFailedException(detail: String) :
    ScimException(status = 412, detail = detail)
