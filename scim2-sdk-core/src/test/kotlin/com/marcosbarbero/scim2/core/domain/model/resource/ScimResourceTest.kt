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
package com.marcosbarbero.scim2.core.domain.model.resource

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.common.Address
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.common.Name
import com.marcosbarbero.scim2.core.domain.vo.ETag
import io.github.serpro69.kfaker.Faker
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

    private val faker = Faker()

    @Nested
    inner class UserTest {

        @Test
        fun `should create User with required userName`() {
            val userName = faker.name.firstName().lowercase()
            val user = User(userName = userName)
            user.userName shouldBe userName
            user.schemas shouldContain ScimUrns.USER
        }

        @Test
        fun `should create User with all attributes`() {
            val now = Instant.now()
            val id = java.util.UUID.randomUUID().toString()
            val externalId = faker.name.firstName().lowercase()
            val email = faker.internet.email()
            val familyName = faker.name.lastName()
            val givenName = faker.name.firstName()
            val middleName = faker.name.firstName()
            val displayName = faker.name.name()
            val nickName = faker.name.firstName()
            val title = faker.name.name()
            val phone = faker.phoneNumber.phoneNumber()
            val streetAddress = faker.address.streetAddress()
            val locality = faker.address.city()
            val region = faker.address.state()
            val postalCode = faker.address.postcode()
            val country = faker.address.country()
            val groupId = java.util.UUID.randomUUID().toString()
            val groupDisplay = faker.name.name()
            val entitlement = faker.name.firstName().lowercase()
            val role = faker.name.firstName().lowercase()
            val user = User(
                id = id,
                externalId = externalId,
                meta = Meta(
                    resourceType = "User",
                    created = now,
                    lastModified = now,
                    location = URI.create("https://example.com/v2/Users/$id"),
                    version = ETag("W/\"${java.util.UUID.randomUUID().toString()}\"")
                ),
                userName = email,
                name = Name(
                    formatted = "Ms. $givenName $middleName $familyName III",
                    familyName = familyName,
                    givenName = givenName,
                    middleName = middleName,
                    honorificPrefix = "Ms.",
                    honorificSuffix = "III"
                ),
                displayName = displayName,
                nickName = nickName,
                profileUrl = URI.create("https://login.example.com/$externalId"),
                title = title,
                userType = "Employee",
                preferredLanguage = "en-US",
                locale = "en-US",
                timezone = "America/Los_Angeles",
                active = true,
                password = null,
                emails = listOf(
                    MultiValuedAttribute(value = email, type = "work", primary = true)
                ),
                phoneNumbers = listOf(
                    MultiValuedAttribute(value = phone, type = "work")
                ),
                ims = listOf(
                    MultiValuedAttribute(value = faker.name.firstName().lowercase(), type = "aim")
                ),
                photos = listOf(
                    MultiValuedAttribute(
                        value = "https://photos.example.com/profilephoto/${java.util.UUID.randomUUID().toString()}/F",
                        type = "photo"
                    )
                ),
                addresses = listOf(
                    Address(
                        streetAddress = streetAddress,
                        locality = locality,
                        region = region,
                        postalCode = postalCode,
                        country = country,
                        type = "work",
                        primary = true
                    )
                ),
                groups = listOf(
                    GroupMembership(value = groupId, display = groupDisplay)
                ),
                entitlements = listOf(
                    MultiValuedAttribute(value = entitlement)
                ),
                roles = listOf(
                    MultiValuedAttribute(value = role)
                ),
                x509Certificates = emptyList()
            )

            user.userName shouldBe email
            user.name.shouldNotBeNull()
            user.name!!.familyName shouldBe familyName
            user.displayName shouldBe displayName
            user.active shouldBe true
            user.emails shouldHaveSize 1
            user.addresses shouldHaveSize 1
            user.groups shouldHaveSize 1
            user.meta.shouldNotBeNull()
            user.meta!!.version.shouldNotBeNull()
        }

        @Test
        fun `should default optional fields to null or empty`() {
            val user = User(userName = faker.name.firstName().lowercase())
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
            val user = User(userName = faker.name.firstName().lowercase())
            val employeeNumber = faker.random.nextInt(100000, 999999).toString()
            val costCenter = faker.random.nextInt(1000, 9999).toString()
            val organization = faker.name.name()
            val division = faker.name.name()
            val department = faker.name.name()
            val ext = EnterpriseUserExtension(
                employeeNumber = employeeNumber,
                costCenter = costCenter,
                organization = organization,
                division = division,
                department = department
            )
            user.setExtension(ScimUrns.ENTERPRISE_USER, ext)

            user.extensions shouldContainKey ScimUrns.ENTERPRISE_USER
            val retrieved = user.getExtension<EnterpriseUserExtension>(
                ScimUrns.ENTERPRISE_USER
            )
            retrieved.shouldNotBeNull()
            retrieved.employeeNumber shouldBe employeeNumber
        }
    }

    @Nested
    inner class GroupTest {

        @Test
        fun `should create Group with required displayName`() {
            val groupName = faker.name.name()
            val group = Group(displayName = groupName)
            group.displayName shouldBe groupName
            group.schemas shouldContain ScimUrns.GROUP
        }

        @Test
        fun `should create Group with members`() {
            val memberId = java.util.UUID.randomUUID().toString()
            val memberDisplay = faker.name.name()
            val group = Group(
                displayName = faker.name.name(),
                members = listOf(
                    GroupMembership(
                        value = memberId,
                        display = memberDisplay,
                        ref = URI.create("https://example.com/v2/Users/$memberId"),
                        type = "User"
                    )
                )
            )

            group.members shouldHaveSize 1
            group.members[0].display shouldBe memberDisplay
        }
    }

    @Nested
    inner class EnterpriseUserExtensionTest {

        @Test
        fun `should create extension with all fields`() {
            val employeeNumber = faker.random.nextInt(100000, 999999).toString()
            val costCenter = faker.random.nextInt(1000, 9999).toString()
            val organization = faker.name.name()
            val division = faker.name.name()
            val department = faker.name.name()
            val managerId = java.util.UUID.randomUUID().toString()
            val managerName = faker.name.name()
            val ext = EnterpriseUserExtension(
                employeeNumber = employeeNumber,
                costCenter = costCenter,
                organization = organization,
                division = division,
                department = department,
                manager = com.marcosbarbero.scim2.core.domain.model.common.Manager(
                    value = managerId,
                    ref = URI.create("https://example.com/v2/Users/$managerId"),
                    displayName = managerName
                )
            )

            ext.employeeNumber shouldBe employeeNumber
            ext.manager.shouldNotBeNull()
            ext.manager!!.displayName shouldBe managerName
        }

        @Test
        fun `should default optional fields to null`() {
            val ext = EnterpriseUserExtension()
            ext.employeeNumber.shouldBeNull()
            ext.manager.shouldBeNull()
        }
    }
}
