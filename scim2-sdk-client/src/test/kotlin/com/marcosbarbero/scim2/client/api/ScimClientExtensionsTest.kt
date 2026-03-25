package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ScimClientExtensionsTest {

    private lateinit var client: ScimClient

    private val userResponse = ScimResponse(
        value = User(id = "u1", userName = "john"),
        statusCode = 200
    )
    private val groupResponse = ScimResponse(
        value = Group(id = "g1", displayName = "admins"),
        statusCode = 200
    )

    @BeforeEach
    fun setUp() {
        client = mockk(relaxed = true)
    }

    // ===== User Operations =====

    @Test
    fun `createUser calls create with Users endpoint and User class`() {
        val user = User(userName = "john")
        val expected = ScimResponse(value = User(id = "u1", userName = "john"), statusCode = 201)
        every { client.create("/Users", user, User::class) } returns expected

        val result = client.createUser(user)

        result shouldBe expected
        verify { client.create("/Users", user, User::class) }
    }

    @Test
    fun `getUser calls get with Users endpoint and User class`() {
        every { client.get("/Users", "u1", User::class) } returns userResponse

        val result = client.getUser("u1")

        result shouldBe userResponse
        verify { client.get("/Users", "u1", User::class) }
    }

    @Test
    fun `replaceUser calls replace with Users endpoint and User class`() {
        val user = User(id = "u1", userName = "updated")
        every { client.replace("/Users", "u1", user, User::class) } returns userResponse

        client.replaceUser("u1", user)

        verify { client.replace("/Users", "u1", user, User::class) }
    }

    @Test
    fun `patchUser calls patch with Users endpoint and User class`() {
        val patch = PatchRequest()
        every { client.patch("/Users", "u1", patch, User::class) } returns userResponse

        client.patchUser("u1", patch)

        verify { client.patch("/Users", "u1", patch, User::class) }
    }

    @Test
    fun `deleteUser calls delete with Users endpoint`() {
        client.deleteUser("u1")

        verify { client.delete("/Users", "u1") }
    }

    @Test
    fun `searchUsers with default request calls search with Users endpoint`() {
        val listResponse = ScimResponse(
            value = ListResponse<User>(totalResults = 0),
            statusCode = 200
        )
        every { client.search("/Users", any<SearchRequest>(), User::class) } returns listResponse

        client.searchUsers()

        verify { client.search("/Users", any<SearchRequest>(), User::class) }
    }

    @Test
    fun `searchUsers with filter string builds SearchRequest`() {
        val listResponse = ScimResponse(
            value = ListResponse<User>(totalResults = 1, resources = listOf(User(userName = "john"))),
            statusCode = 200
        )
        val requestSlot = slot<SearchRequest>()
        every { client.search("/Users", capture(requestSlot), User::class) } returns listResponse

        client.searchUsers("userName sw \"john\"")

        requestSlot.captured.filter shouldBe "userName sw \"john\""
    }

    // ===== Group Operations =====

    @Test
    fun `createGroup calls create with Groups endpoint and Group class`() {
        val group = Group(displayName = "admins")
        val expected = ScimResponse(value = Group(id = "g1", displayName = "admins"), statusCode = 201)
        every { client.create("/Groups", group, Group::class) } returns expected

        val result = client.createGroup(group)

        result shouldBe expected
        verify { client.create("/Groups", group, Group::class) }
    }

    @Test
    fun `getGroup calls get with Groups endpoint and Group class`() {
        every { client.get("/Groups", "g1", Group::class) } returns groupResponse

        val result = client.getGroup("g1")

        result shouldBe groupResponse
        verify { client.get("/Groups", "g1", Group::class) }
    }

    @Test
    fun `replaceGroup calls replace with Groups endpoint and Group class`() {
        val group = Group(id = "g1", displayName = "updated")
        every { client.replace("/Groups", "g1", group, Group::class) } returns groupResponse

        client.replaceGroup("g1", group)

        verify { client.replace("/Groups", "g1", group, Group::class) }
    }

    @Test
    fun `patchGroup calls patch with Groups endpoint and Group class`() {
        val patch = PatchRequest()
        every { client.patch("/Groups", "g1", patch, Group::class) } returns groupResponse

        client.patchGroup("g1", patch)

        verify { client.patch("/Groups", "g1", patch, Group::class) }
    }

    @Test
    fun `deleteGroup calls delete with Groups endpoint`() {
        client.deleteGroup("g1")

        verify { client.delete("/Groups", "g1") }
    }

    @Test
    fun `searchGroups calls search with Groups endpoint`() {
        val listResponse = ScimResponse(
            value = ListResponse<Group>(totalResults = 0),
            statusCode = 200
        )
        every { client.search("/Groups", any<SearchRequest>(), Group::class) } returns listResponse

        client.searchGroups()

        verify { client.search("/Groups", any<SearchRequest>(), Group::class) }
    }

    // ===== Generic typed operations =====

    @Test
    fun `createResource reads ScimResource annotation for User`() {
        val user = User(userName = "john")
        val expected = ScimResponse(value = User(id = "u1", userName = "john"), statusCode = 201)
        every { client.create("/Users", user, User::class) } returns expected

        val result = client.createResource(user)

        result shouldBe expected
        verify { client.create("/Users", user, User::class) }
    }

    @Test
    fun `createResource reads ScimResource annotation for Group`() {
        val group = Group(displayName = "admins")
        val expected = ScimResponse(value = Group(id = "g1", displayName = "admins"), statusCode = 201)
        every { client.create("/Groups", group, Group::class) } returns expected

        val result = client.createResource(group)

        result shouldBe expected
        verify { client.create("/Groups", group, Group::class) }
    }

    @Test
    fun `createResource throws for unannotated class`() {
        val unannotated = object : ScimResource(schemas = listOf("urn:test")) {}

        val exception = shouldThrow<IllegalArgumentException> {
            client.createResource(unannotated)
        }

        exception.message shouldContain "is not annotated with @ScimResource"
    }

    @Test
    fun `getResource reads ScimResource annotation`() {
        every { client.get("/Users", "u1", User::class) } returns userResponse

        val result = client.getResource<User>("u1")

        result shouldBe userResponse
        verify { client.get("/Users", "u1", User::class) }
    }

    @Test
    fun `searchResources reads ScimResource annotation`() {
        val listResponse = ScimResponse(
            value = ListResponse<User>(totalResults = 0),
            statusCode = 200
        )
        every { client.search("/Users", any<SearchRequest>(), User::class) } returns listResponse

        client.searchResources<User>()

        verify { client.search("/Users", any<SearchRequest>(), User::class) }
    }
}
