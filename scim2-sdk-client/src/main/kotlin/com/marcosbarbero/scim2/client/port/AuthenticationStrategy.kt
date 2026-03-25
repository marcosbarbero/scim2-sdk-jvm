package com.marcosbarbero.scim2.client.port

import java.util.Base64

interface AuthenticationStrategy {
    fun authenticate(request: HttpRequest): HttpRequest
}

class BearerTokenAuthentication(private val token: String) : AuthenticationStrategy {
    override fun authenticate(request: HttpRequest): HttpRequest =
        request.copy(headers = request.headers + ("Authorization" to "Bearer $token"))
}

class BasicAuthentication(private val username: String, private val password: String) : AuthenticationStrategy {
    override fun authenticate(request: HttpRequest): HttpRequest {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        return request.copy(headers = request.headers + ("Authorization" to "Basic $encoded"))
    }
}
