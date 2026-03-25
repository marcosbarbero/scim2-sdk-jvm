package com.marcosbarbero.scim2.core.schema.annotation

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.resource.EnterpriseUserExtension
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AnnotationsTest {

    private val faker = Faker()

    @Test
    fun `User class should have ScimResource annotation`() {
        val annotation = User::class.java.getAnnotation(ScimResource::class.java)
        annotation.shouldNotBeNull()
        annotation.schema shouldBe ScimUrns.USER
        annotation.name shouldBe "User"
        annotation.endpoint shouldBe "/Users"
    }

    @Test
    fun `Group class should have ScimResource annotation`() {
        val annotation = Group::class.java.getAnnotation(ScimResource::class.java)
        annotation.shouldNotBeNull()
        annotation.schema shouldBe ScimUrns.GROUP
        annotation.name shouldBe "Group"
        annotation.endpoint shouldBe "/Groups"
    }

    @Test
    fun `EnterpriseUserExtension should have ScimExtension annotation`() {
        val annotation = EnterpriseUserExtension::class.java.getAnnotation(ScimExtension::class.java)
        annotation.shouldNotBeNull()
        annotation.schema shouldBe ScimUrns.ENTERPRISE_USER
    }

    @Test
    fun `AttributeType should have expected values`() {
        AttributeType.entries.map { it.name } shouldBe listOf(
            "STRING", "BOOLEAN", "DECIMAL", "INTEGER", "DATE_TIME", "BINARY", "REFERENCE", "COMPLEX"
        )
    }

    @Test
    fun `Mutability should have expected values`() {
        Mutability.entries.map { it.name } shouldBe listOf(
            "READ_ONLY", "READ_WRITE", "IMMUTABLE", "WRITE_ONLY"
        )
    }

    @Test
    fun `Returned should have expected values`() {
        Returned.entries.map { it.name } shouldBe listOf(
            "ALWAYS", "NEVER", "DEFAULT", "REQUEST"
        )
    }

    @Test
    fun `Uniqueness should have expected values`() {
        Uniqueness.entries.map { it.name } shouldBe listOf(
            "NONE", "SERVER", "GLOBAL"
        )
    }
}
