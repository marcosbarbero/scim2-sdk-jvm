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
package com.marcosbarbero.scim2.core.schema.introspector

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.resource.EnterpriseUserExtension
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.schema.annotation.AttributeType
import com.marcosbarbero.scim2.core.schema.annotation.Mutability
import com.marcosbarbero.scim2.core.schema.annotation.Returned
import com.marcosbarbero.scim2.core.schema.annotation.Uniqueness
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SchemaIntrospectorTest {

    private val faker = Faker()
    private val introspector = SchemaIntrospector()

    @Nested
    inner class IntrospectUserTest {

        @Test
        fun `should introspect User schema with correct id and name`() {
            val schema = introspector.introspect(User::class)
            schema.id shouldBe ScimUrns.USER
            schema.name shouldBe "User"
            schema.description shouldBe "User Account"
        }

        @Test
        fun `should introspect User with all attributes`() {
            val schema = introspector.introspect(User::class)
            val attrNames = schema.attributes.map { it.name }
            attrNames shouldContain "userName"
            attrNames shouldContain "name"
            attrNames shouldContain "displayName"
            attrNames shouldContain "nickName"
            attrNames shouldContain "profileUrl"
            attrNames shouldContain "title"
            attrNames shouldContain "userType"
            attrNames shouldContain "preferredLanguage"
            attrNames shouldContain "locale"
            attrNames shouldContain "timezone"
            attrNames shouldContain "active"
            attrNames shouldContain "password"
            attrNames shouldContain "emails"
            attrNames shouldContain "phoneNumbers"
            attrNames shouldContain "ims"
            attrNames shouldContain "photos"
            attrNames shouldContain "addresses"
            attrNames shouldContain "groups"
            attrNames shouldContain "entitlements"
            attrNames shouldContain "roles"
            attrNames shouldContain "x509Certificates"
        }

        @Test
        fun `should detect userName as required with server uniqueness`() {
            val schema = introspector.introspect(User::class)
            val userName = schema.attributes.first { it.name == "userName" }
            userName.required shouldBe true
            userName.uniqueness shouldBe Uniqueness.SERVER
            userName.type shouldBe AttributeType.STRING
        }

        @Test
        fun `should detect password as writeOnly and never returned`() {
            val schema = introspector.introspect(User::class)
            val password = schema.attributes.first { it.name == "password" }
            password.mutability shouldBe Mutability.WRITE_ONLY
            password.returned shouldBe Returned.NEVER
        }

        @Test
        fun `should detect emails as multi-valued complex`() {
            val schema = introspector.introspect(User::class)
            val emails = schema.attributes.first { it.name == "emails" }
            emails.multiValued shouldBe true
            emails.type shouldBe AttributeType.COMPLEX
            emails.subAttributes.shouldNotBeEmpty()
        }

        @Test
        fun `should detect name as complex with sub-attributes`() {
            val schema = introspector.introspect(User::class)
            val name = schema.attributes.first { it.name == "name" }
            name.type shouldBe AttributeType.COMPLEX
            val subAttrNames = name.subAttributes.map { it.name }
            subAttrNames shouldContain "formatted"
            subAttrNames shouldContain "familyName"
            subAttrNames shouldContain "givenName"
            subAttrNames shouldContain "middleName"
            subAttrNames shouldContain "honorificPrefix"
            subAttrNames shouldContain "honorificSuffix"
        }

        @Test
        fun `should detect active as boolean type`() {
            val schema = introspector.introspect(User::class)
            val active = schema.attributes.first { it.name == "active" }
            active.type shouldBe AttributeType.BOOLEAN
        }

        @Test
        fun `should detect profileUrl as reference type`() {
            val schema = introspector.introspect(User::class)
            val profileUrl = schema.attributes.first { it.name == "profileUrl" }
            profileUrl.type shouldBe AttributeType.REFERENCE
        }
    }

    @Nested
    inner class IntrospectGroupTest {

        @Test
        fun `should introspect Group schema with correct id`() {
            val schema = introspector.introspect(Group::class)
            schema.id shouldBe ScimUrns.GROUP
            schema.name shouldBe "Group"
        }

        @Test
        fun `should detect displayName as required`() {
            val schema = introspector.introspect(Group::class)
            val displayName = schema.attributes.first { it.name == "displayName" }
            displayName.required shouldBe true
        }

        @Test
        fun `should detect members as multi-valued complex`() {
            val schema = introspector.introspect(Group::class)
            val members = schema.attributes.first { it.name == "members" }
            members.multiValued shouldBe true
            members.type shouldBe AttributeType.COMPLEX
            members.subAttributes.shouldNotBeEmpty()
        }
    }

    @Nested
    inner class IntrospectResourceTypeTest {

        @Test
        fun `should introspect User resource type`() {
            val resourceType = introspector.introspectResourceType(User::class)
            resourceType.id shouldBe "User"
            resourceType.name shouldBe "User"
            resourceType.endpoint shouldBe "/Users"
            resourceType.schema shouldBe ScimUrns.USER
        }

        @Test
        fun `should introspect Group resource type`() {
            val resourceType = introspector.introspectResourceType(Group::class)
            resourceType.id shouldBe "Group"
            resourceType.name shouldBe "Group"
            resourceType.endpoint shouldBe "/Groups"
            resourceType.schema shouldBe ScimUrns.GROUP
        }
    }

    @Nested
    inner class IntrospectExtensionTest {

        @Test
        fun `should introspect extension schema`() {
            val schema = introspector.introspect(EnterpriseUserExtension::class)
            schema.id shouldBe ScimUrns.ENTERPRISE_USER
            val attrNames = schema.attributes.map { it.name }
            attrNames shouldContain "employeeNumber"
            attrNames shouldContain "costCenter"
            attrNames shouldContain "organization"
            attrNames shouldContain "division"
            attrNames shouldContain "department"
            attrNames shouldContain "manager"
        }

        @Test
        fun `should detect manager as complex with sub-attributes`() {
            val schema = introspector.introspect(EnterpriseUserExtension::class)
            val manager = schema.attributes.first { it.name == "manager" }
            manager.type shouldBe AttributeType.COMPLEX
            val subAttrNames = manager.subAttributes.map { it.name }
            subAttrNames shouldContain "value"
            subAttrNames shouldContain "displayName"
        }
    }
}
