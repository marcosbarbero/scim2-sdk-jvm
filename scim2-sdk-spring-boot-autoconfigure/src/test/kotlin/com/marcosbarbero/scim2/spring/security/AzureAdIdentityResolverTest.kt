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

class AzureAdIdentityResolverTest {

    private val resolver = AzureAdIdentityResolver()
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
    fun `uses oid as principal`() {
        val jwt = mockJwt(mapOf("oid" to "azure-oid-123", "sub" to "azure-sub"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.principalId shouldBe "azure-oid-123"
    }

    @Test
    fun `falls back to sub when oid missing`() {
        val jwt = mockJwt(mapOf("sub" to "azure-sub-456"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.principalId shouldBe "azure-sub-456"
    }

    @Test
    fun `extracts roles and wids claims`() {
        val jwt = mockJwt(
            mapOf(
                "oid" to "user-1",
                "roles" to listOf("User.Read", "User.Write"),
                "wids" to listOf("global-admin-wid")
            )
        )
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.roles.shouldContainExactlyInAnyOrder("User.Read", "User.Write", "global-admin-wid")
    }

    @Test
    fun `extracts tenant_id from tid claim`() {
        val jwt = mockJwt(mapOf("oid" to "user-1", "tid" to "tenant-abc"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["tenant_id"] shouldBe "tenant-abc"
    }

    @Test
    fun `extracts app_id from appid claim`() {
        val jwt = mockJwt(mapOf("oid" to "user-1", "appid" to "app-xyz"))
        setAuthentication(jwt)

        val context = resolver.resolve(request)

        context.attributes["app_id"] shouldBe "app-xyz"
    }
}
