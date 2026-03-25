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
package com.marcosbarbero.scim2.spring.security

import com.marcosbarbero.scim2.server.port.IdentityResolver
import com.marcosbarbero.scim2.spring.autoconfigure.ScimIdentityAutoConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimIdentityAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ScimIdentityAutoConfiguration::class.java))

    @Test
    fun `provider=keycloak creates KeycloakIdentityResolver`() {
        contextRunner
            .withPropertyValues("scim.idp.provider=keycloak")
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver.shouldNotBeNull()
                resolver.shouldBeInstanceOf<KeycloakIdentityResolver>()
            }
    }

    @Test
    fun `provider=okta creates OktaIdentityResolver`() {
        contextRunner
            .withPropertyValues("scim.idp.provider=okta")
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver.shouldNotBeNull()
                resolver.shouldBeInstanceOf<OktaIdentityResolver>()
            }
    }

    @Test
    fun `provider=azure-ad creates AzureAdIdentityResolver`() {
        contextRunner
            .withPropertyValues("scim.idp.provider=azure-ad")
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver.shouldNotBeNull()
                resolver.shouldBeInstanceOf<AzureAdIdentityResolver>()
            }
    }

    @Test
    fun `provider=ping-federate creates PingFederateIdentityResolver`() {
        contextRunner
            .withPropertyValues("scim.idp.provider=ping-federate")
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver.shouldNotBeNull()
                resolver.shouldBeInstanceOf<PingFederateIdentityResolver>()
            }
    }

    @Test
    fun `provider=auth0 creates Auth0IdentityResolver`() {
        contextRunner
            .withPropertyValues("scim.idp.provider=auth0")
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver.shouldNotBeNull()
                resolver.shouldBeInstanceOf<Auth0IdentityResolver>()
            }
    }

    @Test
    fun `no provider creates default JwtIdentityResolver`() {
        contextRunner
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver.shouldNotBeNull()
                resolver.shouldBeInstanceOf<JwtIdentityResolver>()
            }
    }

    @Test
    fun `custom IdentityResolver bean takes precedence`() {
        val custom = mockk<IdentityResolver>()
        contextRunner
            .withPropertyValues("scim.idp.provider=keycloak")
            .withBean("customResolver", IdentityResolver::class.java, { custom })
            .run { context ->
                val resolver = context.getBean(IdentityResolver::class.java)
                resolver shouldBe custom
            }
    }
}
