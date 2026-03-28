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
package com.marcosbarbero.scim2.core.filter

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class FilterEngineTest {

    private val objectMapper: ObjectMapper = JacksonScimSerializer.defaultObjectMapper()

    private val alice = User(userName = "alice", displayName = "Alice Smith", active = true)
    private val bob = User(userName = "bob", displayName = "Bob Jones", active = false)
    private val charlie = User(userName = "charlie", displayName = "Charlie Smith", active = true)
    private val users = listOf(alice, bob, charlie)

    @Test
    fun `filter by userName eq should return matching user`() {
        val result = FilterEngine.filter(users, "userName eq \"alice\"", objectMapper)

        result shouldHaveSize 1
        result[0].userName shouldBe "alice"
    }

    @Test
    fun `filter by userName sw should return matching users`() {
        val result = FilterEngine.filter(users, "userName sw \"c\"", objectMapper)

        result shouldHaveSize 1
        result[0].userName shouldBe "charlie"
    }

    @Test
    fun `filter by userName co should return matching users`() {
        val result = FilterEngine.filter(users, "userName co \"li\"", objectMapper)

        result shouldHaveSize 2
        result.map { it.userName } shouldBe listOf("alice", "charlie")
    }

    @Test
    fun `filter by active eq true should return active users`() {
        val result = FilterEngine.filter(users, "active eq true", objectMapper)

        result shouldHaveSize 2
        result.map { it.userName } shouldBe listOf("alice", "charlie")
    }

    @Test
    fun `filter with null returns all resources`() {
        val result = FilterEngine.filter(users, null, objectMapper)

        result shouldHaveSize 3
    }

    @Test
    fun `filter with blank returns all resources`() {
        val result = FilterEngine.filter(users, "  ", objectMapper)

        result shouldHaveSize 3
    }

    @Test
    fun `filter by displayName ne should exclude matching`() {
        val result = FilterEngine.filter(users, "displayName ne \"Alice Smith\"", objectMapper)

        result shouldHaveSize 2
        result.map { it.userName } shouldBe listOf("bob", "charlie")
    }

    @Test
    fun `compound filter with and should narrow results`() {
        // "active eq true" matches alice and charlie; "displayName co "Jones"" matches bob
        // AND of these should return 0 because Bob Jones is inactive
        val result = FilterEngine.filter(users, "active eq true and displayName co \"Jones\"", objectMapper)

        result shouldHaveSize 0
    }

    @Test
    fun `compound filter with and returns intersection`() {
        // "active eq true" matches alice and charlie; "displayName co "Smith"" matches alice and charlie
        // AND of these should return both alice and charlie
        val result = FilterEngine.filter(users, "active eq true and displayName co \"Smith\"", objectMapper)

        result shouldHaveSize 2
        result.map { it.userName } shouldBe listOf("alice", "charlie")
    }

    @Test
    fun `compound filter with or should widen results`() {
        val result = FilterEngine.filter(users, "userName eq \"alice\" or userName eq \"bob\"", objectMapper)

        result shouldHaveSize 2
        result.map { it.userName } shouldBe listOf("alice", "bob")
    }
}
