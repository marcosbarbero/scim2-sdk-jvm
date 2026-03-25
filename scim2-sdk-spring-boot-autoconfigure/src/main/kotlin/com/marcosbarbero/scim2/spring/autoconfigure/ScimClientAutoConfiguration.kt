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
package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.client.adapter.httpclient.HttpClientTransport
import com.marcosbarbero.scim2.client.api.ScimClient
import com.marcosbarbero.scim2.client.api.ScimClientBuilder
import com.marcosbarbero.scim2.client.port.AuthenticationStrategy
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [ScimJacksonAutoConfiguration::class])
@ConditionalOnClass(ScimClient::class)
@ConditionalOnProperty(prefix = "scim.client", name = ["base-url"])
@EnableConfigurationProperties(ScimProperties::class)
class ScimClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(HttpTransport::class)
    fun httpTransport(properties: ScimProperties): HttpTransport {
        val httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(properties.client.connectTimeout)
            .build()
        return HttpClientTransport(httpClient, properties.client.readTimeout)
    }

    @Bean
    @ConditionalOnMissingBean(ScimClient::class)
    fun scimClient(
        transport: HttpTransport,
        serializer: ScimSerializer,
        properties: ScimProperties,
        authentication: ObjectProvider<AuthenticationStrategy>
    ): ScimClient = ScimClientBuilder()
        .baseUrl(properties.client.baseUrl!!)
        .transport(transport)
        .serializer(serializer)
        .apply { authentication.ifAvailable?.let { authentication(it) } }
        .build()
}
