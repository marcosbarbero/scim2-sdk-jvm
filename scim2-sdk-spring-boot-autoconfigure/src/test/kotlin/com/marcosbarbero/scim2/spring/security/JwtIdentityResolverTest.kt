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
import io.kotest.matchers.maps.shouldContainKey
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
import java.net.URL

class JwtIdentityResolverTest {

    private val resolver = JwtIdentityResolver()
    private val request = ScimHttpRequest(method = HttpMethod.GET, path = "/scim/v2/Users")

    @BeforeEach
    fun setUp() {
        mockkStatic(SecurityContextHolder::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SecurityContextHolder::class)
    }

    private fun mockJwt(claims: Map<String, Any> = emptyMap()): Jwt {
        val jwt = mockk<Jwt>()
        every { jwt.subject } returns (claims["sub"] as? String)
        every { jwt.issuer } returns (claims["iss"] as? URL)
        every { jwt.getClaimAsString(any()) } answers {
            val claim = firstArg<String>()
            claims[claim] as? String
        }
        every { jwt.getClaimAsStringList(any()) } answers {
            val claim = firstArg<String>()
            @Suppress("UNCHECKED_CAST")
            claims[claim] as? List<String>
        }
        every { jwt.getClaim<Any>(any()) } answers {
            val claim = firstArg<String>()
            claims[claim]
        }
        return jwt
    }

    private fun setAuthentication(jwt: Jwt) {
        val authentication = mockk<JwtAuthenticationToken>()
        every { authentication.token } returns jwt
        val securityContext = mockk<SecurityContext>()
        every { securityContext.authentication } returns authentication
        every { SecurityContextHolder.getContext() } returns securityContext
    }

    private fun setNoAuthentication() {
        val securityContext = mockk<SecurityContext>()
        every { securityContext.authentication } returns null
        every { SecurityContextHolder.getContext() } returns securityContext
    }

    @Test
    fun `extracts principal from sub claim`() {
        val jwt = mockJwt(mapOf("sub" to "user-123"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.principalId shouldBe "user-123"
    }

    @Test
    fun `extracts roles from roles claim`() {
        val jwt = mockJwt(mapOf("sub" to "user-1", "roles" to listOf("admin", "reader")))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles shouldBe setOf("admin", "reader")
    }

    @Test
    fun `extracts email attribute`() {
        val jwt = mockJwt(mapOf("sub" to "user-1", "email" to "user@example.com"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes shouldContainKey "email"
        context.attributes["email"] shouldBe "user@example.com"
    }

    @Test
    fun `handles missing claims gracefully`() {
        val jwt = mockJwt(emptyMap())
        every { jwt.subject } returns null
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.principalId shouldBe "unknown"
        context.roles shouldBe emptySet()
    }

    @Test
    fun `handles no authentication in SecurityContext`() {
        setNoAuthentication()

        val context = resolver.resolve(request)

        context.principalId shouldBe null
        context.roles shouldBe emptySet()
        context.attributes shouldBe emptyMap()
    }

    @Test
    fun `extracts name and issuer attributes`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "user-1",
                "name" to "John Doe",
                "iss" to java.net.URI.create("https://issuer.example.com").toURL(),
            ),
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["name"] shouldBe "John Doe"
        context.attributes["issuer"] shouldBe "https://issuer.example.com"
    }
}
