package com.marcosbarbero.scim2.spring.persistence

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.server.port.ResourceRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Tests that scim.persistence.schema-name properly configures Hibernate's default schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresCustomSchemaTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withInitScript("create-custom-schema.sql")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("scim.persistence.enabled") { "true" }
            registry.add("scim.persistence.schema-name") { "custom_scim" }
            registry.add("spring.flyway.enabled") { "false" }
        }
    }

    @Autowired
    private lateinit var userRepository: ResourceRepository<User>

    @Test
    fun `CRUD works with custom schema name`() {
        val user = User(userName = "schema-test-user")
        val created = userRepository.create(user)
        created.id.shouldNotBeNull()

        val found = userRepository.findById(created.id!!)
        found.shouldNotBeNull()
    }
}
