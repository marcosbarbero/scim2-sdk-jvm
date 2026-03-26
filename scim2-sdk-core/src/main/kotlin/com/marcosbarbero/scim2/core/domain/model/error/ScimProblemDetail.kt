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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * RFC 9457 Problem Detail representation, extended with SCIM-specific fields.
 *
 * When Content-Type is application/problem+json, this format is used.
 * When Content-Type is application/scim+json, the standard ScimError format is used.
 * Both carry the same information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScimProblemDetail(
    val type: String = "about:blank",
    val title: String? = null,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    @JsonProperty("scimType")
    val scimType: String? = null,
) {
    companion object {
        private const val SCIM_ERROR_TYPE_PREFIX = "urn:ietf:params:scim:api:messages:2.0:Error:"

        fun fromScimException(ex: ScimException): ScimProblemDetail = ScimProblemDetail(
            type = ex.scimType?.let { "$SCIM_ERROR_TYPE_PREFIX${it.value}" } ?: "about:blank",
            title = ex.scimType?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() }
                ?: httpStatusTitle(ex.status),
            status = ex.status,
            detail = ex.detail,
            scimType = ex.scimType?.value,
        )

        fun fromScimError(error: ScimError): ScimProblemDetail = ScimProblemDetail(
            type = error.scimType?.let { "$SCIM_ERROR_TYPE_PREFIX$it" } ?: "about:blank",
            title = error.scimType ?: httpStatusTitle(error.status.toIntOrNull() ?: 500),
            status = error.status.toIntOrNull() ?: 500,
            detail = error.detail,
            scimType = error.scimType,
        )

        internal fun httpStatusTitle(status: Int): String = when (status) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            409 -> "Conflict"
            412 -> "Precondition Failed"
            413 -> "Payload Too Large"
            500 -> "Internal Server Error"
            501 -> "Not Implemented"
            else -> "Error"
        }
    }
}
