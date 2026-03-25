package com.marcosbarbero.scim2.core.domain.model.resource

import com.marcosbarbero.scim2.core.domain.model.common.Address
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.common.Name
import com.marcosbarbero.scim2.core.domain.vo.ETag
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant

class ScimResourceTest {

    @Nested
    inner class UserTest {

        @Test
        fun `should create User with required userName`() {
            val user = User(userName = "bjensen")
            user.userName shouldBe "bjensen"
            user.schemas shouldContain "urn:ietf:params:scim:schemas:core:2.0:User"
        }

        @Test
        fun `should create User with all attributes`() {
            val now = Instant.now()
            val user = User(
                id = "2819c223-7f76-453a-919d-413861904646",
                externalId = "bjensen",
                meta = Meta(
                    resourceType = "User",
                    created = now,
                    lastModified = now,
                    location = URI.create("https://example.com/v2/Users/2819c223"),
                    version = ETag("W/\"a330bc54f0671c9\"")
                ),
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
                password = null,
                emails = listOf(
                    MultiValuedAttribute(value = "bjensen@example.com", type = "work", primary = true)
                ),
                phoneNumbers = listOf(
                    MultiValuedAttribute(value = "555-555-5555", type = "work")
                ),
                ims = listOf(
                    MultiValuedAttribute(value = "someaimhandle", type = "aim")
                ),
                photos = listOf(
                    MultiValuedAttribute(
                        value = "https://photos.example.com/profilephoto/72930000000Ccne/F",
                        type = "photo"
                    )
                ),
                addresses = listOf(
                    Address(
                        streetAddress = "100 Universal City Plaza",
                        locality = "Hollywood",
                        region = "CA",
                        postalCode = "91608",
                        country = "US",
                        type = "work",
                        primary = true
                    )
                ),
                groups = listOf(
                    GroupMembership(value = "e9e30dba-f08f-4109-8486-d5c6a331660a", display = "Tour Guides")
                ),
                entitlements = listOf(
                    MultiValuedAttribute(value = "admin")
                ),
                roles = listOf(
                    MultiValuedAttribute(value = "manager")
                ),
                x509Certificates = emptyList()
            )

            user.userName shouldBe "bjensen@example.com"
            user.name.shouldNotBeNull()
            user.name!!.familyName shouldBe "Jensen"
            user.displayName shouldBe "Babs Jensen"
            user.active shouldBe true
            user.emails shouldHaveSize 1
            user.addresses shouldHaveSize 1
            user.groups shouldHaveSize 1
            user.meta.shouldNotBeNull()
            user.meta!!.version.shouldNotBeNull()
        }

        @Test
        fun `should default optional fields to null or empty`() {
            val user = User(userName = "simple")
            user.name.shouldBeNull()
            user.displayName.shouldBeNull()
            user.active.shouldBeNull()
            user.emails shouldBe emptyList()
            user.phoneNumbers shouldBe emptyList()
            user.addresses shouldBe emptyList()
            user.groups shouldBe emptyList()
        }

        @Test
        fun `should support extensions via map`() {
            val user = User(userName = "bjensen")
            val ext = EnterpriseUserExtension(
                employeeNumber = "701984",
                costCenter = "4130",
                organization = "Universal Studios",
                division = "Theme Park",
                department = "Tour Operations"
            )
            user.setExtension("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User", ext)

            user.extensions shouldContainKey "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
            val retrieved = user.getExtension<EnterpriseUserExtension>(
                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
            )
            retrieved.shouldNotBeNull()
            retrieved.employeeNumber shouldBe "701984"
        }
    }

    @Nested
    inner class GroupTest {

        @Test
        fun `should create Group with required displayName`() {
            val group = Group(displayName = "Tour Guides")
            group.displayName shouldBe "Tour Guides"
            group.schemas shouldContain "urn:ietf:params:scim:schemas:core:2.0:Group"
        }

        @Test
        fun `should create Group with members`() {
            val group = Group(
                displayName = "Tour Guides",
                members = listOf(
                    GroupMembership(
                        value = "2819c223-7f76-453a-919d-413861904646",
                        display = "Babs Jensen",
                        ref = URI.create("https://example.com/v2/Users/2819c223"),
                        type = "User"
                    )
                )
            )

            group.members shouldHaveSize 1
            group.members[0].display shouldBe "Babs Jensen"
        }
    }

    @Nested
    inner class EnterpriseUserExtensionTest {

        @Test
        fun `should create extension with all fields`() {
            val ext = EnterpriseUserExtension(
                employeeNumber = "701984",
                costCenter = "4130",
                organization = "Universal Studios",
                division = "Theme Park",
                department = "Tour Operations",
                manager = com.marcosbarbero.scim2.core.domain.model.common.Manager(
                    value = "26118915-6090-4610-87e4-49d8ca9f808d",
                    ref = URI.create("https://example.com/v2/Users/26118915"),
                    displayName = "John Smith"
                )
            )

            ext.employeeNumber shouldBe "701984"
            ext.manager.shouldNotBeNull()
            ext.manager!!.displayName shouldBe "John Smith"
        }

        @Test
        fun `should default optional fields to null`() {
            val ext = EnterpriseUserExtension()
            ext.employeeNumber.shouldBeNull()
            ext.manager.shouldBeNull()
        }
    }
}
