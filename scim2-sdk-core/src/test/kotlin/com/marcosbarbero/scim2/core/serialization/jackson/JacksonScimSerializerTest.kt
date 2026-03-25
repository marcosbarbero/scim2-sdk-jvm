package com.marcosbarbero.scim2.core.serialization.jackson

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.common.Address
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.common.Name
import com.marcosbarbero.scim2.core.domain.model.resource.EnterpriseUserExtension
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.vo.ETag
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant

class JacksonScimSerializerTest {

    private val faker = Faker()
    private val serializer = JacksonScimSerializer()

    @Nested
    inner class UserSerializationTest {

        @Test
        fun `should serialize and deserialize User round-trip`() {
            val id = java.util.UUID.randomUUID().toString()
            val email = faker.internet.email()
            val familyName = faker.name.lastName()
            val givenName = faker.name.firstName()
            val displayName = faker.name.name()
            val user = User(
                id = id,
                userName = email,
                name = Name(
                    formatted = "$givenName $familyName",
                    familyName = familyName,
                    givenName = givenName
                ),
                displayName = displayName,
                active = true,
                emails = listOf(
                    MultiValuedAttribute(value = email, type = "work", primary = true)
                )
            )

            val json = serializer.serializeToString(user)
            val deserialized = serializer.deserializeFromString(json, User::class)

            deserialized.id shouldBe user.id
            deserialized.userName shouldBe user.userName
            deserialized.displayName shouldBe user.displayName
            deserialized.active shouldBe true
            deserialized.emails.size shouldBe 1
            deserialized.emails[0].value shouldBe email
        }

        @Test
        fun `should exclude null fields from serialized output`() {
            val userName = faker.name.firstName().lowercase()
            val user = User(userName = userName)
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
                userName = faker.name.firstName().lowercase(),
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
            val email = faker.internet.email()
            val familyName = faker.name.lastName()
            val givenName = faker.name.firstName()
            val streetAddress = faker.address.streetAddress()
            val locality = faker.address.city()
            val region = faker.address.state()
            val groupId = java.util.UUID.randomUUID().toString()
            val groupDisplay = faker.name.name()
            val phone = faker.phoneNumber.phoneNumber()
            val user = User(
                id = java.util.UUID.randomUUID().toString(),
                externalId = java.util.UUID.randomUUID().toString(),
                userName = email,
                name = Name(
                    formatted = "Ms. $givenName $familyName III",
                    familyName = familyName,
                    givenName = givenName,
                    middleName = faker.name.firstName(),
                    honorificPrefix = "Ms.",
                    honorificSuffix = "III"
                ),
                displayName = faker.name.name(),
                nickName = faker.name.firstName(),
                profileUrl = URI.create("https://login.example.com/${faker.name.firstName().lowercase()}"),
                title = faker.name.name(),
                userType = "Employee",
                preferredLanguage = "en-US",
                locale = "en-US",
                timezone = "America/Los_Angeles",
                active = true,
                emails = listOf(MultiValuedAttribute(value = email, type = "work", primary = true)),
                phoneNumbers = listOf(MultiValuedAttribute(value = phone, type = "work")),
                addresses = listOf(Address(streetAddress = streetAddress, locality = locality, region = region)),
                groups = listOf(GroupMembership(value = groupId, display = groupDisplay))
            )

            val json = serializer.serializeToString(user)
            val deserialized = serializer.deserializeFromString(json, User::class)

            deserialized.userName shouldBe email
            deserialized.name?.familyName shouldBe familyName
            deserialized.addresses.size shouldBe 1
            deserialized.groups.size shouldBe 1
        }
    }

    @Nested
    inner class GroupSerializationTest {

        @Test
        fun `should serialize and deserialize Group round-trip`() {
            val groupId = java.util.UUID.randomUUID().toString()
            val groupName = faker.name.name()
            val memberId = java.util.UUID.randomUUID().toString()
            val memberDisplay = faker.name.name()
            val group = Group(
                id = groupId,
                displayName = groupName,
                members = listOf(
                    GroupMembership(
                        value = memberId,
                        display = memberDisplay,
                        ref = URI.create("https://example.com/v2/Users/$memberId"),
                        type = "User"
                    )
                )
            )

            val json = serializer.serializeToString(group)
            val deserialized = serializer.deserializeFromString(json, Group::class)

            deserialized.displayName shouldBe groupName
            deserialized.members.size shouldBe 1
            deserialized.members[0].display shouldBe memberDisplay
        }
    }

    @Nested
    inner class ExtensionSerializationTest {

        @Test
        fun `should serialize and deserialize User with extension data`() {
            val employeeNumber = faker.random.nextInt(100000, 999999).toString()
            val costCenter = faker.random.nextInt(1000, 9999).toString()
            val organization = faker.name.name()
            val user = User(userName = faker.name.firstName().lowercase())
            user.setExtension(
                ScimUrns.ENTERPRISE_USER,
                mapOf(
                    "employeeNumber" to employeeNumber,
                    "costCenter" to costCenter,
                    "organization" to organization
                )
            )

            val json = serializer.serializeToString(user)
            json shouldContain ScimUrns.ENTERPRISE_USER
            json shouldContain employeeNumber

            val deserialized = serializer.deserializeFromString(json, User::class)
            val ext = deserialized.getExtension<Map<*, *>>(
                ScimUrns.ENTERPRISE_USER
            )
            ext shouldBe mapOf(
                "employeeNumber" to employeeNumber,
                "costCenter" to costCenter,
                "organization" to organization
            )
        }
    }

    @Nested
    inner class ByteArraySerializationTest {

        @Test
        fun `should serialize to and deserialize from byte array`() {
            val userName = faker.name.firstName().lowercase()
            val displayName = faker.name.name()
            val user = User(userName = userName, displayName = displayName)
            val bytes = serializer.serialize(user)
            val deserialized = serializer.deserialize(bytes, User::class)

            deserialized.userName shouldBe userName
            deserialized.displayName shouldBe displayName
        }
    }
}
