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
        val scimRequest = request.toScimHttpRequest()
        val scimResponse = dispatcher.dispatch(scimRequest)
        return scimResponse.toResponseEntity()
    }

    private fun HttpServletRequest.toScimHttpRequest(): ScimHttpRequest {
        val method = HttpMethod.valueOf(this.method.uppercase())
        val path = requestURI
        val headers = mutableMapOf<String, List<String>>()
        val headerNames = this.headerNames
        while (headerNames.hasMoreElements()) {
            val name = headerNames.nextElement()
            headers[name] = getHeaders(name).toList()
        }
        val queryParameters = mutableMapOf<String, List<String>>()
        parameterMap.forEach { (key, values) ->
            queryParameters[key] = values.toList()
        }
        val body = inputStream.readAllBytes().takeIf { it.isNotEmpty() }

        return ScimHttpRequest(
            method = method,
            path = path,
            headers = headers,
            queryParameters = queryParameters,
            body = body
        )
    }

    private fun ScimHttpResponse.toResponseEntity(): ResponseEntity<ByteArray> {
        val headers = HttpHeaders()
        this.headers.forEach { (key, value) -> headers.add(key, value) }
        return ResponseEntity
            .status(status)
            .headers(headers)
            .body(body)
    }
}
