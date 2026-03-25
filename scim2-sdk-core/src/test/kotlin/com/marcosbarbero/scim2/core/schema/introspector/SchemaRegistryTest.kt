package com.marcosbarbero.scim2.core.schema.introspector

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.resource.EnterpriseUserExtension
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SchemaRegistryTest {

    private val faker = Faker()
    private lateinit var registry: SchemaRegistry

    @BeforeEach
    fun setUp() {
        registry = SchemaRegistry()
    }

    @Nested
    inner class RegistrationTest {

        @Test
        fun `should register and retrieve User schema`() {
            registry.register(User::class)
            val schema = registry.getSchema(ScimUrns.USER)
            schema.shouldNotBeNull()
            schema.id shouldBe ScimUrns.USER
        }

        @Test
        fun `should register and retrieve Group schema`() {
            registry.register(Group::class)
            val schema = registry.getSchema(ScimUrns.GROUP)
            schema.shouldNotBeNull()
            schema.id shouldBe ScimUrns.GROUP
        }

        @Test
        fun `should return null for unknown schema URI`() {
            registry.getSchema("urn:${faker.name.name()}").shouldBeNull()
        }

        @Test
        fun `should retrieve resource type by name`() {
            registry.register(User::class)
            val resourceType = registry.getResourceType("User")
            resourceType.shouldNotBeNull()
            resourceType.name shouldBe "User"
            resourceType.endpoint shouldBe "/Users"
        }

        @Test
        fun `should return null for unknown resource type`() {
            registry.getResourceType(faker.name.name()).shouldBeNull()
        }

        @Test
        fun `should list all registered schemas`() {
            registry.register(User::class)
            registry.register(Group::class)
            registry.getAllSchemas() shouldHaveSize 2
        }

        @Test
        fun `should list all registered resource types`() {
            registry.register(User::class)
            registry.register(Group::class)
            registry.getAllResourceTypes() shouldHaveSize 2
        }
    }

    @Nested
    inner class ExtensionRegistrationTest {

        @Test
        fun `should register extension and add to resource type`() {
            registry.register(User::class)
            registry.registerExtension(User::class, EnterpriseUserExtension::class)

            val resourceType = registry.getResourceType("User")
            resourceType.shouldNotBeNull()
            resourceType.schemaExtensions shouldHaveSize 1
            resourceType.schemaExtensions[0].schema shouldBe
                ScimUrns.ENTERPRISE_USER
        }

        @Test
        fun `should register extension schema in schema registry`() {
            registry.register(User::class)
            registry.registerExtension(User::class, EnterpriseUserExtension::class)

            val extSchema = registry.getSchema(
                ScimUrns.ENTERPRISE_USER
            )
            extSchema.shouldNotBeNull()
            extSchema.attributes.map { it.name }.toSet() shouldBe setOf(
                "employeeNumber", "costCenter", "organization", "division", "department", "manager"
            )
        }
    }

    @Nested
    inner class ThreadSafetyTest {

        @Test
        fun `should handle concurrent reads safely`() {
            registry.register(User::class)
            registry.register(Group::class)

            val threadCount = 10
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val results = mutableListOf<Boolean>()

            repeat(threadCount) {
                executor.submit {
                    try {
                        val user = registry.getSchema(ScimUrns.USER)
                        val group = registry.getSchema(ScimUrns.GROUP)
                        val allSchemas = registry.getAllSchemas()
                        val allTypes = registry.getAllResourceTypes()
                        synchronized(results) {
                            results.add(
                                user != null && group != null &&
                                    allSchemas.size == 2 && allTypes.size == 2
                            )
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()
            results shouldHaveSize threadCount
            results.all { it } shouldBe true
        }
    }
}
