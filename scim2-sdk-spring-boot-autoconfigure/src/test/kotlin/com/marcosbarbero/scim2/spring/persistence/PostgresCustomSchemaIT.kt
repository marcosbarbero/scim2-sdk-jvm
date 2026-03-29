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
package com.marcosbarbero.scim2.spring.persistence

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.server.port.ResourceRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Tests that scim.persistence.schema-name properly configures Hibernate's default schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresCustomSchemaIT {

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
