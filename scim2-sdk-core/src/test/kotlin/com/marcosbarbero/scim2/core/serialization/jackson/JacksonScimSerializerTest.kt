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
package com.marcosbarbero.scim2.core.serialization.jackson

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.common.Address
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.common.Name
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.ServiceProviderConfig
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
            val id =
                java.util.UUID
                    .randomUUID()
                    .toString()
            val email = faker.internet.email()
            val familyName = faker.name.lastName()
            val givenName = faker.name.firstName()
            val displayName = faker.name.name()
            val user =
                User(
                    id = id,
                    userName = email,
                    name =
                    Name(
                        formatted = "$givenName $familyName",
                        familyName = familyName,
                        givenName = givenName,
                    ),
                    displayName = displayName,
                    active = true,
                    emails =
                    listOf(
                        MultiValuedAttribute(value = email, type = "work", primary = true),
                    ),
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
            val user =
                User(
                    userName = faker.name.firstName().lowercase(),
                    meta =
                    Meta(
                        created = instant,
                        lastModified = instant,
                    ),
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
            val groupId =
                java.util.UUID
                    .randomUUID()
                    .toString()
            val groupDisplay = faker.name.name()
            val phone = faker.phoneNumber.phoneNumber()
            val user =
                User(
                    id =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                    externalId =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                    userName = email,
                    name =
                    Name(
                        formatted = "Ms. $givenName $familyName III",
                        familyName = familyName,
                        givenName = givenName,
                        middleName = faker.name.firstName(),
                        honorificPrefix = "Ms.",
                        honorificSuffix = "III",
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
                    groups = listOf(GroupMembership(value = groupId, display = groupDisplay)),
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
            val groupId =
                java.util.UUID
                    .randomUUID()
                    .toString()
            val groupName = faker.name.name()
            val memberId =
                java.util.UUID
                    .randomUUID()
                    .toString()
            val memberDisplay = faker.name.name()
            val group =
                Group(
                    id = groupId,
                    displayName = groupName,
                    members =
                    listOf(
                        GroupMembership(
                            value = memberId,
                            display = memberDisplay,
                            ref = URI.create("https://example.com/v2/Users/$memberId"),
                            type = "User",
                        ),
                    ),
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
                    "organization" to organization,
                ),
            )

            val json = serializer.serializeToString(user)
            json shouldContain ScimUrns.ENTERPRISE_USER
            json shouldContain employeeNumber

            val deserialized = serializer.deserializeFromString(json, User::class)
            val ext =
                deserialized.getExtension<Map<*, *>>(
                    ScimUrns.ENTERPRISE_USER,
                )
            ext shouldBe
                mapOf(
                    "employeeNumber" to employeeNumber,
                    "costCenter" to costCenter,
                    "organization" to organization,
                )
        }
    }

    @Nested
    inner class DiscoverySerializationTest {
        @Test
        fun `should serialize ResourceType with schemas attribute`() {
            val rt = ResourceType(
                id = "User",
                name = "User",
                description = "User Account",
                endpoint = "/Users",
                schema = ScimUrns.USER,
            )

            val json = serializer.serializeToString(rt)

            json shouldContain "\"schemas\""
            json shouldContain ScimUrns.RESOURCE_TYPE

            val deserialized = serializer.deserializeFromString(json, ResourceType::class)
            deserialized.schemas shouldBe listOf(ScimUrns.RESOURCE_TYPE)
            deserialized.name shouldBe "User"
        }

        @Test
        fun `should serialize ServiceProviderConfig with schemas attribute`() {
            val spc = ServiceProviderConfig(
                patch = ServiceProviderConfig.SupportedConfig(supported = true),
                filter = ServiceProviderConfig.FilterConfig(supported = true, maxResults = 200),
            )

            val json = serializer.serializeToString(spc)

            json shouldContain "\"schemas\""
            json shouldContain ScimUrns.SERVICE_PROVIDER_CONFIG

            val deserialized = serializer.deserializeFromString(json, ServiceProviderConfig::class)
            deserialized.schemas shouldBe listOf(ScimUrns.SERVICE_PROVIDER_CONFIG)
            deserialized.patch.supported shouldBe true
        }
    }

    @Nested
    inner class EmptyCollectionSerializationTest {
        @Test
        fun `should include empty lists in serialized output`() {
            val user = User(userName = "emptytest")
            val json = serializer.serializeToString(user)

            // NON_NULL inclusion: empty lists are valid SCIM values and should be serialized
            json shouldContain "\"emails\""
            json shouldContain "\"phoneNumbers\""
            json shouldContain "\"addresses\""
            json shouldContain "\"groups\""
        }

        @Test
        fun `should include non-empty lists in serialized output`() {
            val user = User(
                userName = "nonemptytest",
                emails = listOf(
                    MultiValuedAttribute(value = "test@example.com", type = "work", primary = true),
                ),
            )
            val json = serializer.serializeToString(user)

            json shouldContain "\"emails\""
            json shouldContain "test@example.com"
        }

        @Test
        fun `should omit null fields but not empty strings`() {
            val user = User(userName = "nulltest")
            val json = serializer.serializeToString(user)

            // Null fields are omitted
            json shouldNotContain "\"displayName\""
            json shouldNotContain "\"nickName\""
            json shouldContain "\"userName\""
        }

        @Test
        fun `should preserve empty string values`() {
            val user = User(userName = "test", displayName = "")
            val json = serializer.serializeToString(user)

            // Empty strings are valid SCIM attribute values and must not be suppressed
            json shouldContain "\"displayName\""
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

    @Nested
    inner class EnrichMetaLocationTest {
        @Test
        fun `should add meta location to JSON without existing meta`() {
            val user = User(userName = "test")
            val bytes = serializer.serialize(user)

            val enriched = serializer.enrichMetaLocation(bytes, "https://example.com/scim/v2/Users/123")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"location\":\"https://example.com/scim/v2/Users/123\""
        }

        @Test
        fun `should add meta location to JSON with existing meta`() {
            val user = User(
                userName = "test",
                meta = Meta(created = Instant.parse("2025-04-01T12:00:00Z")),
            )
            val bytes = serializer.serialize(user)

            val enriched = serializer.enrichMetaLocation(bytes, "https://example.com/scim/v2/Users/123")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"location\":\"https://example.com/scim/v2/Users/123\""
            json shouldContain "2025-04-01T12:00:00Z"
        }

        @Test
        fun `should set resourceType when not already present`() {
            val user = User(userName = "test")
            val bytes = serializer.serialize(user)

            val enriched = serializer.enrichMetaLocation(bytes, "https://example.com/scim/v2/Users/123", "User")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"resourceType\":\"User\""
        }

        @Test
        fun `should not overwrite existing resourceType`() {
            val user = User(
                userName = "test",
                meta = Meta(resourceType = "OriginalType"),
            )
            val bytes = serializer.serialize(user)

            val enriched = serializer.enrichMetaLocation(bytes, "https://example.com/scim/v2/Users/123", "User")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"resourceType\":\"OriginalType\""
            json shouldNotContain "\"resourceType\":\"User\""
        }

        @Test
        fun `should not set resourceType when parameter is null`() {
            val user = User(userName = "test")
            val bytes = serializer.serialize(user)

            val enriched = serializer.enrichMetaLocation(bytes, "https://example.com/scim/v2/Users/123", null)
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"location\""
            json shouldNotContain "\"resourceType\""
        }
    }

    @Nested
    inner class FilterReturnedNeverTest {
        @Test
        fun `should exclude returned NEVER attributes from serialized output`() {
            val user = User(userName = "secret.user", password = "s3cret!")
            val bytes = serializer.serialize(user)

            val filtered = serializer.filterReturnedNever(bytes, User::class.java)
            val json = String(filtered, Charsets.UTF_8)

            json shouldNotContain "\"password\""
            json shouldNotContain "s3cret!"
        }

        @Test
        fun `should preserve other attributes when filtering returned NEVER`() {
            val user = User(
                userName = "visible.user",
                displayName = "Visible User",
                password = "s3cret!",
                emails = listOf(MultiValuedAttribute(value = "test@example.com", type = "work")),
            )
            val bytes = serializer.serialize(user)

            val filtered = serializer.filterReturnedNever(bytes, User::class.java)
            val json = String(filtered, Charsets.UTF_8)

            json shouldContain "\"userName\""
            json shouldContain "visible.user"
            json shouldContain "\"displayName\""
            json shouldContain "Visible User"
            json shouldContain "\"emails\""
            json shouldContain "test@example.com"
            json shouldNotContain "\"password\""
        }
    }

    @Nested
    inner class EnrichMemberRefsTest {
        @Test
        fun `should add ref to group members`() {
            val group = Group(
                displayName = "Engineering",
                members = listOf(GroupMembership(value = "user-123", display = "Jane", type = "User")),
            )
            val bytes = serializer.serialize(group)

            val enriched = serializer.enrichMemberRefs(bytes, "https://example.com/scim/v2")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"\$ref\":\"https://example.com/scim/v2/Users/user-123\""
        }

        @Test
        fun `should not overwrite existing ref`() {
            val group = Group(
                displayName = "Engineering",
                members = listOf(
                    GroupMembership(
                        value = "user-123",
                        display = "Jane",
                        type = "User",
                        ref = URI.create("https://other.com/scim/v2/Users/user-123"),
                    ),
                ),
            )
            val bytes = serializer.serialize(group)

            val enriched = serializer.enrichMemberRefs(bytes, "https://example.com/scim/v2")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "https://other.com/scim/v2/Users/user-123"
            json shouldNotContain "https://example.com"
        }

        @Test
        fun `should add ref to user groups`() {
            val user = User(
                userName = "jane",
                groups = listOf(GroupMembership(value = "group-456", display = "Admins", type = "Group")),
            )
            val bytes = serializer.serialize(user)

            val enriched = serializer.enrichMemberRefs(bytes, "https://example.com/scim/v2")
            val json = String(enriched, Charsets.UTF_8)

            json shouldContain "\"\$ref\":\"https://example.com/scim/v2/Groups/group-456\""
        }
    }
}
