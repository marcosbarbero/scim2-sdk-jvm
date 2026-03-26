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
package com.marcosbarbero.scim2.test.server

import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class InMemoryScimServerTest {

    private val faker = Faker()
    private val objectMapper = jacksonObjectMapper()
    private val server = InMemoryScimServer()

    @BeforeEach
    fun setUp() {
        server.reset()
    }

    @Test
    fun `createUser returns 201 with created user`() {
        val user = User(userName = faker.name.firstName())

        val response = server.createUser(user)

        response.status shouldBe 201
        val body = objectMapper.readTree(response.body)
        body.get("id").asText() shouldNotBe null
    }

    @Test
    fun `getUser returns 200 for existing user`() {
        val createResponse = server.createUser(User(userName = faker.name.firstName()))
        val id = objectMapper.readTree(createResponse.body).get("id").asText()

        val response = server.getUser(id)

        response.status shouldBe 200
    }

    @Test
    fun `getUser returns 404 for non-existent user`() {
        val response = server.getUser("nonexistent")

        response.status shouldBe 404
    }

    @Test
    fun `replaceUser returns 200`() {
        val createResponse = server.createUser(User(userName = faker.name.firstName()))
        val id = objectMapper.readTree(createResponse.body).get("id").asText()
        val newUserName = faker.name.firstName()

        val response = server.replaceUser(id, User(userName = newUserName))

        response.status shouldBe 200
        val body = objectMapper.readTree(response.body)
        body.get("userName").asText() shouldBe newUserName
    }

    @Test
    fun `deleteUser returns 204`() {
        val createResponse = server.createUser(User(userName = faker.name.firstName()))
        val id = objectMapper.readTree(createResponse.body).get("id").asText()

        val response = server.deleteUser(id)

        response.status shouldBe 204
    }

    @Test
    fun `searchUsers returns 200 with results`() {
        server.createUser(User(userName = faker.name.firstName()))
        server.createUser(User(userName = faker.name.firstName()))

        val response = server.searchUsers()

        response.status shouldBe 200
        val body = objectMapper.readTree(response.body)
        body.get("totalResults").asInt() shouldBe 2
    }

    @Test
    fun `searchUsers with pagination parameters`() {
        repeat(5) { server.createUser(User(userName = faker.name.firstName())) }

        val response = server.searchUsers(startIndex = 2, count = 2)

        response.status shouldBe 200
    }

    @Test
    fun `createGroup returns 201`() {
        val group = Group(displayName = faker.name.lastName())

        val response = server.createGroup(group)

        response.status shouldBe 201
    }

    @Test
    fun `getGroup returns 200 for existing group`() {
        val createResponse = server.createGroup(Group(displayName = faker.name.lastName()))
        val id = objectMapper.readTree(createResponse.body).get("id").asText()

        val response = server.getGroup(id)

        response.status shouldBe 200
    }

    @Test
    fun `deleteGroup returns 204`() {
        val createResponse = server.createGroup(Group(displayName = faker.name.lastName()))
        val id = objectMapper.readTree(createResponse.body).get("id").asText()

        val response = server.deleteGroup(id)

        response.status shouldBe 204
    }

    @Test
    fun `reset clears all repositories`() {
        server.createUser(User(userName = faker.name.firstName()))
        server.createGroup(Group(displayName = faker.name.lastName()))

        server.reset()

        server.userRepository.count() shouldBe 0
        server.groupRepository.count() shouldBe 0
    }

    @Test
    fun `dispatch routes arbitrary requests`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "${server.config.basePath}/ServiceProviderConfig"
        )

        val response = server.dispatch(request)

        response.status shouldBe 200
    }
}
