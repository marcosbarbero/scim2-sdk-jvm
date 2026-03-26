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
package com.marcosbarbero.scim2.client.port

import java.util.Base64

interface AuthenticationStrategy {
    fun authenticate(request: HttpRequest): HttpRequest
}

class BearerTokenAuthentication(private val token: String) : AuthenticationStrategy {
    override fun authenticate(request: HttpRequest): HttpRequest = request.copy(headers = request.headers + ("Authorization" to "Bearer $token"))
}

class BasicAuthentication(private val username: String, private val password: String) : AuthenticationStrategy {
    override fun authenticate(request: HttpRequest): HttpRequest {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        return request.copy(headers = request.headers + ("Authorization" to "Basic $encoded"))
    }
}
