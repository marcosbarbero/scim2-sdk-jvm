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
package com.marcosbarbero.scim2.core.domain.model.error

import com.marcosbarbero.scim2.core.domain.ScimUrns

open class ScimException(
    val status: Int,
    val scimType: ScimErrorType? = null,
    val detail: String? = null
) : RuntimeException(detail) {

    fun toScimError(): ScimError = ScimError(
        schemas = listOf(ScimUrns.ERROR),
        status = status.toString(),
        scimType = scimType?.value,
        detail = detail
    )

    fun toScimProblemDetail(): ScimProblemDetail = ScimProblemDetail.fromScimException(this)
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
