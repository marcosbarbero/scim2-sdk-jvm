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
import com.marcosbarbero.scim2.client.port.BearerTokenAuthentication
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.spring.event.SpringScimEventPublisher
import com.marcosbarbero.scim2.spring.provisioning.ScimProvisioningEventListener
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [ScimServerAutoConfiguration::class, ScimJacksonAutoConfiguration::class])
@EnableConfigurationProperties(ScimProperties::class)
class ScimProvisioningAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ScimEventPublisher::class)
    fun springScimEventPublisher(applicationEventPublisher: ApplicationEventPublisher): ScimEventPublisher =
        SpringScimEventPublisher(applicationEventPublisher)

    @Bean
    @ConditionalOnProperty(prefix = "scim.provisioning", name = ["enabled"], havingValue = "true")
    @ConditionalOnClass(ScimClient::class)
    fun scimProvisioningClient(
        serializer: ScimSerializer,
        properties: ScimProperties,
    ): ScimClient {
        val provisioningProps = properties.provisioning
        val targetUrl = requireNotNull(provisioningProps.targetUrl) {
            "scim.provisioning.target-url must be set when scim.provisioning.enabled=true"
        }
        val builder = ScimClientBuilder()
            .baseUrl(targetUrl)
            .transport(HttpClientTransport(java.net.http.HttpClient.newHttpClient(), properties.client.readTimeout))
            .serializer(serializer)

        provisioningProps.bearerToken?.let {
            builder.authentication(BearerTokenAuthentication(it))
        }

        return builder.build()
    }

    @Bean
    @ConditionalOnProperty(prefix = "scim.provisioning", name = ["enabled"], havingValue = "true")
    fun scimProvisioningEventListener(
        scimProvisioningClient: ScimClient,
        handlers: ObjectProvider<ResourceHandler<*>>,
    ): ScimProvisioningEventListener = ScimProvisioningEventListener(
        client = scimProvisioningClient,
        handlers = handlers.orderedStream().toList(),
    )
}
