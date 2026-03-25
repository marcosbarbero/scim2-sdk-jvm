package com.marcosbarbero.scim2.core.serialization.jackson

import com.marcosbarbero.scim2.core.domain.model.common.Address
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.common.Name
import com.marcosbarbero.scim2.core.domain.model.resource.EnterpriseUserExtension
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.vo.ETag
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant

class JacksonScimSerializerTest {

    private val serializer = JacksonScimSerializer()

    @Nested
    inner class UserSerializationTest {

        @Test
        fun `should serialize and deserialize User round-trip`() {
            val user = User(
                id = "2819c223-7f76-453a-919d-413861904646",
                userName = "bjensen@example.com",
                name = Name(
                    formatted = "Ms. Barbara J Jensen III",
                    familyName = "Jensen",
                    givenName = "Barbara"
                ),
                displayName = "Babs Jensen",
                active = true,
                emails = listOf(
                    MultiValuedAttribute(value = "bjensen@example.com", type = "work", primary = true)
                )
            )

            val json = serializer.serializeToString(user)
            val deserialized = serializer.deserializeFromString(json, User::class)

            deserialized.id shouldBe user.id
            deserialized.userName shouldBe user.userName
            deserialized.displayName shouldBe user.displayName
            deserialized.active shouldBe true
            deserialized.emails.size shouldBe 1
            deserialized.emails[0].value shouldBe "bjensen@example.com"
        }

        @Test
        fun `should exclude null fields from serialized output`() {
            val user = User(userName = "bjensen")
            val json = serializer.serializeToString(user)

            json shouldNotContain "\"displayName\""
            json shouldNotContain "\"nickName\""
            json shouldNotContain "\"title\""
            json shouldContain "\"userName\""
            json shouldContain "\"schemas\""
        }

        @Test
        fun `should serialize Instant as ISO-8601`() {
            val instant = Instant.parse("2025-04-01T12:00:00Z")
            val user = User(
                userName = "bjensen",
                meta = Meta(
                    created = instant,
                    lastModified = instant
                )
            )

            val json = serializer.serializeToString(user)
            json shouldContain "2025-04-01T12:00:00Z"
            json shouldNotContain "1743505200"
        }

        @Test
        fun `should serialize User with all attributes`() {
            val user = User(
                id = "123",
                externalId = "ext-123",
                userName = "bjensen@example.com",
                name = Name(
                    formatted = "Ms. Barbara J Jensen III",
                    familyName = "Jensen",
                    givenName = "Barbara",
                    middleName = "Jane",
                    honorificPrefix = "Ms.",
                    honorificSuffix = "III"
                ),
                displayName = "Babs Jensen",
                nickName = "Babs",
                profileUrl = URI.create("https://login.example.com/bjensen"),
                title = "Tour Guide",
                userType = "Employee",
                preferredLanguage = "en-US",
                locale = "en-US",
                timezone = "America/Los_Angeles",
                active = true,
                emails = listOf(MultiValuedAttribute(value = "bjensen@example.com", type = "work", primary = true)),
                phoneNumbers = listOf(MultiValuedAttribute(value = "555-555-5555", type = "work")),
                addresses = listOf(Address(streetAddress = "100 Universal City Plaza", locality = "Hollywood", region = "CA")),
                groups = listOf(GroupMembership(value = "group-1", display = "Admins"))
            )

            val json = serializer.serializeToString(user)
            val deserialized = serializer.deserializeFromString(json, User::class)

            deserialized.userName shouldBe "bjensen@example.com"
            deserialized.name?.familyName shouldBe "Jensen"
            deserialized.addresses.size shouldBe 1
            deserialized.groups.size shouldBe 1
        }
    }

    @Nested
    inner class GroupSerializationTest {

        @Test
        fun `should serialize and deserialize Group round-trip`() {
            val group = Group(
                id = "e9e30dba-f08f-4109-8486-d5c6a331660a",
                displayName = "Tour Guides",
                members = listOf(
                    GroupMembership(
                        value = "2819c223",
                        display = "Babs Jensen",
                        ref = URI.create("https://example.com/v2/Users/2819c223"),
                        type = "User"
                    )
                )
            )

            val json = serializer.serializeToString(group)
            val deserialized = serializer.deserializeFromString(json, Group::class)

            deserialized.displayName shouldBe "Tour Guides"
            deserialized.members.size shouldBe 1
            deserialized.members[0].display shouldBe "Babs Jensen"
        }
    }

    @Nested
    inner class ExtensionSerializationTest {

        @Test
        fun `should serialize and deserialize User with extension data`() {
            val user = User(userName = "bjensen")
            user.setExtension(
                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                mapOf(
                    "employeeNumber" to "701984",
                    "costCenter" to "4130",
                    "organization" to "Universal Studios"
                )
            )

            val json = serializer.serializeToString(user)
            json shouldContain "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
            json shouldContain "701984"

            val deserialized = serializer.deserializeFromString(json, User::class)
            val ext = deserialized.getExtension<Map<*, *>>(
                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
            )
            ext shouldBe mapOf(
                "employeeNumber" to "701984",
                "costCenter" to "4130",
                "organization" to "Universal Studios"
            )
        }
    }

    @Nested
    inner class ByteArraySerializationTest {

        @Test
        fun `should serialize to and deserialize from byte array`() {
            val user = User(userName = "bjensen", displayName = "Babs")
            val bytes = serializer.serialize(user)
            val deserialized = serializer.deserialize(bytes, User::class)

            deserialized.userName shouldBe "bjensen"
            deserialized.displayName shouldBe "Babs"
        }
    }
}
