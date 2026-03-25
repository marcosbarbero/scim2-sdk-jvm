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

import com.marcosbarbero.scim2.server.port.IdentityResolver
import com.marcosbarbero.scim2.spring.security.Auth0IdentityResolver
import com.marcosbarbero.scim2.spring.security.AzureAdIdentityResolver
import com.marcosbarbero.scim2.spring.security.JwtIdentityResolver
import com.marcosbarbero.scim2.spring.security.KeycloakIdentityResolver
import com.marcosbarbero.scim2.spring.security.OktaIdentityResolver
import com.marcosbarbero.scim2.spring.security.PingFederateIdentityResolver
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration that registers an [IdentityResolver] based on the configured IdP provider.
 * Activates only when Spring Security OAuth2 JWT classes are on the classpath.
 * Each provider bean backs off via [ConditionalOnMissingBean] so users can always provide
 * their own [IdentityResolver].
 *
 * All claim names are configurable via `scim.idp.claims.*` properties.
 */
@AutoConfiguration
@ConditionalOnClass(name = ["org.springframework.security.oauth2.jwt.Jwt"])
@EnableConfigurationProperties(ScimProperties::class)
class ScimIdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdentityResolver::class)
    @ConditionalOnProperty(prefix = "scim.idp", name = ["provider"], havingValue = "keycloak")
    fun keycloakIdentityResolver(properties: ScimProperties): IdentityResolver =
        KeycloakIdentityResolver(clientId = properties.idp.clientId, claims = properties.idp.claims)

    @Bean
    @ConditionalOnMissingBean(IdentityResolver::class)
    @ConditionalOnProperty(prefix = "scim.idp", name = ["provider"], havingValue = "okta")
    fun oktaIdentityResolver(properties: ScimProperties): IdentityResolver =
        OktaIdentityResolver(claims = properties.idp.claims)

    @Bean
    @ConditionalOnMissingBean(IdentityResolver::class)
    @ConditionalOnProperty(prefix = "scim.idp", name = ["provider"], havingValue = "azure-ad")
    fun azureAdIdentityResolver(properties: ScimProperties): IdentityResolver =
        AzureAdIdentityResolver(claims = properties.idp.claims)

    @Bean
    @ConditionalOnMissingBean(IdentityResolver::class)
    @ConditionalOnProperty(prefix = "scim.idp", name = ["provider"], havingValue = "ping-federate")
    fun pingFederateIdentityResolver(properties: ScimProperties): IdentityResolver =
        PingFederateIdentityResolver(claims = properties.idp.claims)

    @Bean
    @ConditionalOnMissingBean(IdentityResolver::class)
    @ConditionalOnProperty(prefix = "scim.idp", name = ["provider"], havingValue = "auth0")
    fun auth0IdentityResolver(properties: ScimProperties): IdentityResolver =
        Auth0IdentityResolver(
            namespace = properties.idp.namespace ?: "https://your-app.auth0.com",
            claims = properties.idp.claims
        )

    @Bean
    @ConditionalOnMissingBean(IdentityResolver::class)
    fun defaultJwtIdentityResolver(properties: ScimProperties): IdentityResolver =
        JwtIdentityResolver(claims = properties.idp.claims)
}
