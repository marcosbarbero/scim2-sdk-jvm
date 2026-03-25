package com.marcosbarbero.scim2.spring.web

import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${scim.base-path:/scim/v2}")
class ScimController(private val dispatcher: ScimEndpointDispatcher) {

    @RequestMapping("/**")
    fun handle(request: HttpServletRequest): ResponseEntity<ByteArray> {
        val scimRequest = toScimHttpRequest(request)
        val scimResponse = dispatcher.dispatch(scimRequest)
        return toResponseEntity(scimResponse)
    }

    private fun toScimHttpRequest(request: HttpServletRequest): ScimHttpRequest {
        val method = HttpMethod.valueOf(request.method.uppercase())
        val path = request.requestURI
        val headers = mutableMapOf<String, List<String>>()
        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val name = headerNames.nextElement()
            headers[name] = request.getHeaders(name).toList()
        }
        val queryParameters = mutableMapOf<String, List<String>>()
        request.parameterMap.forEach { (key, values) ->
            queryParameters[key] = values.toList()
        }
        val body = request.inputStream.readAllBytes().takeIf { it.isNotEmpty() }

        return ScimHttpRequest(
            method = method,
            path = path,
            headers = headers,
            queryParameters = queryParameters,
            body = body
        )
    }

    private fun toResponseEntity(response: ScimHttpResponse): ResponseEntity<ByteArray> {
        val headers = HttpHeaders()
        response.headers.forEach { (key, value) -> headers.add(key, value) }
        return ResponseEntity
            .status(response.status)
            .headers(headers)
            .body(response.body)
    }
}
