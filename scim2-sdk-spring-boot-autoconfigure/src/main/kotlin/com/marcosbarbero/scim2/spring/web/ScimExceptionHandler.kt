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
package com.marcosbarbero.scim2.spring.web

import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ScimExceptionHandler(private val serializer: ScimSerializer) {

    @ExceptionHandler(ScimException::class)
    fun handleScimException(ex: ScimException, request: HttpServletRequest): ResponseEntity<ByteArray> {
        val acceptsProblemJson = request.getHeader("Accept")
            ?.contains("application/problem+json") == true

        return if (acceptsProblemJson) {
            val problemDetail = ex.toScimProblemDetail()
            val body = serializer.serialize(problemDetail)
            ResponseEntity.status(ex.status)
                .contentType(MediaType("application", "problem+json"))
                .body(body)
        } else {
            val error = ex.toScimError()
            val body = serializer.serialize(error)
            ResponseEntity.status(ex.status)
                .contentType(MediaType("application", "scim+json"))
                .body(body)
        }
    }
}
