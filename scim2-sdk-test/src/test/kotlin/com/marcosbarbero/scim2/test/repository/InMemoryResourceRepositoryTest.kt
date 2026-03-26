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
package com.marcosbarbero.scim2.test.repository

import com.marcosbarbero.scim2.core.domain.model.error.ResourceConflictException
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import io.github.serpro69.kfaker.Faker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemoryResourceRepositoryTest {

    private val faker = Faker()

    private val repository = InMemoryResourceRepository<User> { user, id, meta ->
        user.copy(id = id, meta = meta)
    }

    @BeforeEach
    fun setUp() {
        repository.clear()
    }

    @Test
    fun `create stores resource with generated id and meta`() {
        val user = User(userName = faker.name.firstName())

        val created = repository.create(user)

        created.id.shouldNotBeNull()
        created.meta.shouldNotBeNull()
        created.meta!!.version.shouldNotBeNull()
        repository.count() shouldBe 1
    }

    @Test
    fun `create with duplicate id throws ResourceConflictException`() {
        val user = User(id = "fixed-id", userName = faker.name.firstName())
        repository.create(user)

        shouldThrow<ResourceConflictException> {
            repository.create(User(id = "fixed-id", userName = faker.name.firstName()))
        }
    }

    @Test
    fun `findById returns null for non-existent id`() {
        repository.findById("nonexistent").shouldBeNull()
    }

    @Test
    fun `findById returns resource for existing id`() {
        val created = repository.create(User(userName = faker.name.firstName()))

        val found = repository.findById(created.id!!)

        found.shouldNotBeNull()
        found.id shouldBe created.id
    }

    @Test
    fun `replace updates resource and meta`() {
        val created = repository.create(User(userName = faker.name.firstName()))
        val newUserName = faker.name.firstName()

        val replaced = repository.replace(created.id!!, User(userName = newUserName), null)

        replaced.id shouldBe created.id
        replaced.userName shouldBe newUserName
        replaced.meta.shouldNotBeNull()
    }

    @Test
    fun `replace throws ResourceNotFoundException for non-existent id`() {
        shouldThrow<ResourceNotFoundException> {
            repository.replace("nonexistent", User(userName = faker.name.firstName()), null)
        }
    }

    @Test
    fun `replace with version mismatch throws ResourceConflictException`() {
        val created = repository.create(User(userName = faker.name.firstName()))

        shouldThrow<ResourceConflictException> {
            repository.replace(created.id!!, User(userName = faker.name.firstName()), "wrong-version")
        }
    }

    @Test
    fun `delete removes resource`() {
        val created = repository.create(User(userName = faker.name.firstName()))

        repository.delete(created.id!!, null)

        repository.count() shouldBe 0
        repository.findById(created.id!!).shouldBeNull()
    }

    @Test
    fun `delete throws ResourceNotFoundException for non-existent id`() {
        shouldThrow<ResourceNotFoundException> {
            repository.delete("nonexistent", null)
        }
    }

    @Test
    fun `delete with version mismatch throws ResourceConflictException`() {
        val created = repository.create(User(userName = faker.name.firstName()))

        shouldThrow<ResourceConflictException> {
            repository.delete(created.id!!, "wrong-version")
        }
    }

    @Test
    fun `search returns all resources with default pagination`() {
        repeat(5) { repository.create(User(userName = faker.name.firstName())) }

        val result = repository.search(SearchRequest())

        result.totalResults shouldBe 5
        result.resources shouldHaveSize 5
    }

    @Test
    fun `search with startIndex and count paginates correctly`() {
        repeat(5) { repository.create(User(userName = faker.name.firstName())) }

        val result = repository.search(SearchRequest(startIndex = 2, count = 2))

        result.totalResults shouldBe 5
        result.resources shouldHaveSize 2
        result.startIndex shouldBe 2
    }

    @Test
    fun `search with startIndex beyond size returns empty`() {
        repeat(3) { repository.create(User(userName = faker.name.firstName())) }

        val result = repository.search(SearchRequest(startIndex = 10, count = 10))

        result.totalResults shouldBe 3
        result.resources.shouldBeEmpty()
    }

    @Test
    fun `count returns number of stored resources`() {
        repository.count() shouldBe 0

        repository.create(User(userName = faker.name.firstName()))
        repository.create(User(userName = faker.name.firstName()))

        repository.count() shouldBe 2
    }

    @Test
    fun `clear removes all resources`() {
        repeat(3) { repository.create(User(userName = faker.name.firstName())) }

        repository.clear()

        repository.count() shouldBe 0
        repository.getAll().shouldBeEmpty()
    }

    @Test
    fun `getAll returns all stored resources`() {
        repeat(3) { repository.create(User(userName = faker.name.firstName())) }

        repository.getAll() shouldHaveSize 3
    }

    @Test
    fun `createWithId stores resource with specific id`() {
        val user = User(userName = faker.name.firstName())
        val id = "custom-id"

        val created = repository.createWithId(id, user)

        created.id shouldBe id
        repository.findById(id).shouldNotBeNull()
    }

    @Test
    fun `deleteIfExists removes resource silently`() {
        val created = repository.create(User(userName = faker.name.firstName()))

        repository.deleteIfExists(created.id!!)

        repository.count() shouldBe 0
    }

    @Test
    fun `deleteIfExists does not throw for non-existent id`() {
        repository.deleteIfExists("nonexistent")

        repository.count() shouldBe 0
    }
}
