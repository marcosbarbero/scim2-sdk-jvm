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
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.server.port.ResourceRepository
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration test proving JPA persistence works against a real PostgreSQL database.
 * Tests the full lifecycle: create, read, update, delete.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresIT {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("scim.persistence.enabled") { "true" }
            registry.add("spring.flyway.enabled") { "false" }
        }
    }

    @Autowired
    private lateinit var userRepository: ResourceRepository<User>

    private val faker = Faker()

    @Test
    fun `create and retrieve user on PostgreSQL`() {
        val user = User(userName = faker.name.firstName().lowercase())
        val created = userRepository.create(user)
        created.id.shouldNotBeNull()

        val found = userRepository.findById(created.id!!)
        found.shouldNotBeNull()
        found.userName shouldBe user.userName
    }

    @Test
    fun `replace user on PostgreSQL`() {
        val user = User(userName = faker.name.firstName().lowercase())
        val created = userRepository.create(user)

        val updated = User(userName = created.userName, displayName = faker.name.name())
        val replaced = userRepository.replace(created.id!!, updated, null)
        replaced.displayName shouldBe updated.displayName
    }

    @Test
    fun `delete user on PostgreSQL`() {
        val user = User(userName = faker.name.firstName().lowercase())
        val created = userRepository.create(user)

        userRepository.delete(created.id!!, null)

        val found = userRepository.findById(created.id!!)
        found.shouldBeNull()
    }

    @Test
    fun `search users on PostgreSQL`() {
        repeat(3) {
            userRepository.create(User(userName = faker.name.firstName().lowercase() + it))
        }

        val results = userRepository.search(SearchRequest(startIndex = 1, count = 10))
        results.totalResults shouldBe results.resources.size
    }
}
