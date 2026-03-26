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
package com.marcosbarbero.scim2.server.engine

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkOperation
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

class BulkEngineTest {

    private val faker = Faker()
    private val objectMapper = jacksonObjectMapper()
    private val config = ScimServerConfig()
    private val context = ScimRequestContext()
    private val users = mutableMapOf<String, User>()

    private val serializer = object : ScimSerializer {
        override fun <T : Any> serialize(value: T): ByteArray = objectMapper.writeValueAsBytes(value)
        override fun <T : Any> deserialize(bytes: ByteArray, type: KClass<T>): T = objectMapper.readValue(bytes, type.java)
        override fun serializeToString(value: Any): String = objectMapper.writeValueAsString(value)
        override fun <T : Any> deserializeFromString(json: String, type: KClass<T>): T = objectMapper.readValue(json, type.java)
    }

    private val userHandler = object : ResourceHandler<User> {
        override val resourceType: Class<User> = User::class.java
        override val endpoint: String = "/Users"

        override fun get(id: String, context: ScimRequestContext): User = users[id] ?: throw ResourceNotFoundException("User not found: $id")

        override fun create(resource: User, context: ScimRequestContext): User {
            val id = java.util.UUID.randomUUID().toString()
            val created = resource.copy(id = id)
            users[id] = created
            return created
        }

        override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User {
            val replaced = resource.copy(id = id)
            users[id] = replaced
            return replaced
        }

        override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User {
            val existing = users[id] ?: throw ResourceNotFoundException("User not found: $id")
            return existing
        }

        override fun delete(id: String, version: String?, context: ScimRequestContext) {
            users.remove(id) ?: throw ResourceNotFoundException("User not found: $id")
        }

        override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> = ListResponse(totalResults = users.size, resources = users.values.toList())
    }

    private val engine = BulkEngine(listOf(userHandler), serializer, config)

    @Test
    fun `execute should process POST operation`() {
        val userName = faker.name.firstName()
        val userData = objectMapper.valueToTree<tools.jackson.databind.JsonNode>(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to userName),
        )
        val request = BulkRequest(
            operations = listOf(
                BulkOperation(method = "POST", path = "/Users", bulkId = "user1", data = userData),
            ),
        )

        val response = engine.execute(request, context)

        response.operations.size shouldBe 1
        response.operations[0].status shouldBe "201"
        response.operations[0].bulkId shouldBe "user1"
    }

    @Test
    fun `execute should process DELETE operation`() {
        val id = java.util.UUID.randomUUID().toString()
        users[id] = User(id = id, userName = faker.name.firstName())

        val request = BulkRequest(
            operations = listOf(
                BulkOperation(method = "DELETE", path = "/Users/$id"),
            ),
        )

        val response = engine.execute(request, context)

        response.operations.size shouldBe 1
        response.operations[0].status shouldBe "204"
    }

    @Test
    fun `execute should resolve bulkId cross-references`() {
        val userName1 = faker.name.firstName()
        val userName2 = faker.name.firstName()
        val user1Data = objectMapper.valueToTree<tools.jackson.databind.JsonNode>(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to userName1),
        )
        val user2Data = objectMapper.valueToTree<tools.jackson.databind.JsonNode>(
            mapOf("schemas" to listOf(ScimUrns.USER), "userName" to userName2),
        )

        val request = BulkRequest(
            operations = listOf(
                BulkOperation(method = "POST", path = "/Users", bulkId = "user1", data = user1Data),
                BulkOperation(method = "POST", path = "/Users", bulkId = "user2", data = user2Data),
            ),
        )

        val response = engine.execute(request, context)

        response.operations.size shouldBe 2
        response.operations[0].status shouldBe "201"
        response.operations[1].status shouldBe "201"
    }

    @Test
    fun `execute should stop after failOnErrors threshold`() {
        val request = BulkRequest(
            failOnErrors = 1,
            operations = listOf(
                BulkOperation(method = "DELETE", path = "/Users/nonexistent1"),
                BulkOperation(method = "DELETE", path = "/Users/nonexistent2"),
                BulkOperation(method = "DELETE", path = "/Users/nonexistent3"),
            ),
        )

        val response = engine.execute(request, context)

        response.operations.size shouldBe 1
        response.operations[0].status shouldBe "404"
    }

    @Test
    fun `execute should reject requests exceeding max operations`() {
        val tinyConfig = config.copy(bulkMaxOperations = 2)
        val smallEngine = BulkEngine(listOf(userHandler), serializer, tinyConfig)

        val request = BulkRequest(
            operations = (1..5).map {
                BulkOperation(method = "DELETE", path = "/Users/${java.util.UUID.randomUUID()}")
            },
        )

        assertThrows<IllegalArgumentException> {
            smallEngine.execute(request, context)
        }
    }

    @Test
    fun `execute should return 404 for unknown endpoint`() {
        val request = BulkRequest(
            operations = listOf(
                BulkOperation(method = "DELETE", path = "/Unknown/123"),
            ),
        )

        val response = engine.execute(request, context)

        response.operations[0].status shouldBe "404"
    }
}
