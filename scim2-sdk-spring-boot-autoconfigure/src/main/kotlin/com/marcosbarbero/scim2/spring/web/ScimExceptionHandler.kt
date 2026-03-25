package com.marcosbarbero.scim2.spring.web

import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ScimExceptionHandler(private val serializer: ScimSerializer) {

    @ExceptionHandler(ScimException::class)
    fun handleScimException(ex: ScimException): ResponseEntity<ByteArray> {
        val error = ex.toScimError()
        val body = serializer.serialize(error)
        return ResponseEntity.status(ex.status)
            .contentType(MediaType("application", "scim+json"))
            .body(body)
    }
}
