package com.marcosbarbero.scim2.spring.web

import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
open class ScimExceptionHandler(private val serializer: ScimSerializer) {

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
