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

import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class KeycloakIdentityResolverTest {

    private val resolver = KeycloakIdentityResolver(clientId = "my-client")
    private val request = ScimHttpRequest(method = HttpMethod.GET, path = "/scim/v2/Users")

    @BeforeEach
    fun setUp() {
        mockkStatic(SecurityContextHolder::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SecurityContextHolder::class)
    }

    private fun mockJwt(claims: Map<String, Any>): Jwt {
        val jwt = mockk<Jwt>()
        every { jwt.subject } returns (claims["sub"] as? String)
        every { jwt.issuer } returns null
        every { jwt.getClaimAsString(any()) } answers {
            claims[firstArg<String>()] as? String
        }
        every { jwt.getClaimAsStringList(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            claims[firstArg<String>()] as? List<String>
        }
        every { jwt.getClaim<Any>(any()) } answers {
            claims[firstArg<String>()]
        }
        return jwt
    }

    private fun setAuthentication(jwt: Jwt) {
        val auth = mockk<JwtAuthenticationToken>()
        every { auth.token } returns jwt
        val ctx = mockk<SecurityContext>()
        every { ctx.authentication } returns auth
        every { SecurityContextHolder.getContext() } returns ctx
    }

    @Test
    fun `extracts realm_access roles`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "kc-user",
                "realm_access" to mapOf("roles" to listOf("realm-admin", "user")),
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("realm-admin", "user")
    }

    @Test
    fun `extracts resource_access client roles`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "kc-user",
                "realm_access" to mapOf("roles" to listOf("user")),
                "resource_access" to mapOf(
                    "my-client" to mapOf("roles" to listOf("client-admin")),
                ),
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("user", "client-admin")
    }

    @Test
    fun `extracts preferred_username attribute`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "kc-user",
                "preferred_username" to "johndoe",
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["preferred_username"] shouldBe "johndoe"
    }

    @Test
    fun `extracts given_name and family_name attributes`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "kc-user",
                "given_name" to "John",
                "family_name" to "Doe",
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["given_name"] shouldBe "John"
        context.attributes["family_name"] shouldBe "Doe"
    }
}
