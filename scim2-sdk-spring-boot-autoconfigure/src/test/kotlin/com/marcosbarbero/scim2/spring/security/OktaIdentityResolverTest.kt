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

class OktaIdentityResolverTest {

    private val resolver = OktaIdentityResolver()
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
    fun `extracts groups claim as roles`() {
        val jwt = mockJwt(mapOf("sub" to "okta-user", "groups" to listOf("Everyone", "Admins")))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("Everyone", "Admins")
    }

    @Test
    fun `extracts scp claim as roles`() {
        val jwt = mockJwt(mapOf("sub" to "okta-user", "scp" to listOf("openid", "profile")))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("openid", "profile")
    }

    @Test
    fun `combines groups and scp claims`() {
        val jwt = mockJwt(
            mapOf(
                "sub" to "okta-user",
                "groups" to listOf("Admins"),
                "scp" to listOf("openid")
            )
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("Admins", "openid")
    }

    @Test
    fun `extracts principal from sub`() {
        val jwt = mockJwt(mapOf("sub" to "okta-user-123"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.principalId shouldBe "okta-user-123"
    }
}
