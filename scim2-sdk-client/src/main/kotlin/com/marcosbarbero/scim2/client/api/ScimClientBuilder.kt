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
package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.client.port.AuthenticationStrategy
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer

class ScimClientBuilder {
    private var baseUrl: String? = null
    private var transport: HttpTransport? = null
    private var serializer: ScimSerializer? = null
    private var authentication: AuthenticationStrategy? = null
    private var defaultHeaders: Map<String, String> = emptyMap()

    fun baseUrl(url: String): ScimClientBuilder = apply { this.baseUrl = url }

    fun transport(transport: HttpTransport): ScimClientBuilder = apply { this.transport = transport }

    fun serializer(serializer: ScimSerializer): ScimClientBuilder = apply { this.serializer = serializer }

    fun authentication(auth: AuthenticationStrategy): ScimClientBuilder = apply { this.authentication = auth }

    fun defaultHeaders(headers: Map<String, String>): ScimClientBuilder = apply { this.defaultHeaders = headers }

    fun build(): ScimClient {
        val resolvedBaseUrl = requireNotNull(baseUrl) { "baseUrl must be set" }.trimEnd('/')
        val resolvedTransport = requireNotNull(transport) { "transport must be set" }
        val resolvedSerializer = requireNotNull(serializer) { "serializer must be set" }

        return DefaultScimClient(
            baseUrl = resolvedBaseUrl,
            transport = resolvedTransport,
            serializer = resolvedSerializer,
            authentication = authentication,
            defaultHeaders = defaultHeaders
        )
    }
}
