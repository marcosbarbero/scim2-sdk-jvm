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
