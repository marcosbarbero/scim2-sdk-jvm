package com.marcosbarbero.scim2.core.domain.model.error

open class ScimException(
    val status: Int,
    val scimType: ScimErrorType? = null,
    val detail: String? = null
) : RuntimeException(detail) {

    fun toScimError(): ScimError = ScimError(
        schemas = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
        status = status.toString(),
        scimType = scimType?.value,
        detail = detail
    )
}

class ResourceNotFoundException(detail: String? = null) :
    ScimException(status = 404, detail = detail)

class ResourceConflictException(detail: String? = null) :
    ScimException(status = 409, scimType = ScimErrorType.UNIQUENESS, detail = detail)

class InvalidFilterException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.INVALID_FILTER, detail = detail)

class InvalidPathException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.INVALID_PATH, detail = detail)

class MutabilityException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.MUTABILITY, detail = detail)

class InvalidSyntaxException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.INVALID_SYNTAX, detail = detail)

class NoTargetException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.NO_TARGET, detail = detail)

class InvalidValueException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.INVALID_VALUE, detail = detail)

class TooManyException(detail: String? = null) :
    ScimException(status = 400, scimType = ScimErrorType.TOO_MANY, detail = detail)

class SensitiveException(detail: String? = null) :
    ScimException(status = 403, scimType = ScimErrorType.SENSITIVE, detail = detail)
