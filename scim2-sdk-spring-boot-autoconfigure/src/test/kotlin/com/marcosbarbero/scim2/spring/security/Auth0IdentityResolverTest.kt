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

class Auth0IdentityResolverTest {

    private val resolver = Auth0IdentityResolver(namespace = "https://my-app.auth0.com")
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
    fun `extracts roles from namespace roles claim`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "auth0|user-1",
                "https://my-app.auth0.com/roles" to listOf("admin", "editor"),
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("admin", "editor")
    }

    @Test
    fun `extracts permissions claim`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "auth0|user-1",
                "permissions" to listOf("read:users", "write:users"),
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("read:users", "write:users")
    }

    @Test
    fun `combines namespace roles and permissions`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "auth0|user-1",
                "https://my-app.auth0.com/roles" to listOf("admin"),
                "permissions" to listOf("read:users"),
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("admin", "read:users")
    }

    @Test
    fun `extracts nickname attribute`() {
        val jwt = mockJwt(mapOf("sub" to "auth0|user-1", "nickname" to "johndoe"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["nickname"] shouldBe "johndoe"
    }

    @Test
    fun `extracts picture attribute`() {
        val jwt = mockJwt(mapOf("sub" to "auth0|user-1", "picture" to "https://cdn.example.com/avatar.png"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["picture"] shouldBe "https://cdn.example.com/avatar.png"
    }
}
