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
package com.marcosbarbero.scim2.sample.spring

import com.marcosbarbero.scim2.client.adapter.httpclient.HttpClientTransport
import com.marcosbarbero.scim2.client.api.ScimClient
import com.marcosbarbero.scim2.client.api.ScimClientBuilder
import com.marcosbarbero.scim2.client.api.create
import com.marcosbarbero.scim2.client.api.replace
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * E2E test proving that SCIM CRUD operations trigger Spring ApplicationEvents
 * via the SpringScimEventPublisher bridge. This validates the event pipeline
 * that outbox adapters (namastack-outbox, custom JDBC) depend on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScimEventBridgeE2E.TestConfig::class)
class ScimEventBridgeE2E(@LocalServerPort val port: Int) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun permitAllFilterChain(http: HttpSecurity): SecurityFilterChain = http
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
            .build()

        @Bean
        fun scimEventCollector(): ScimEventCollector = ScimEventCollector()
    }

    class ScimEventCollector {
        private val _events = mutableListOf<ScimEvent>()
        val events: List<ScimEvent> get() = _events.toList()

        @EventListener
        fun onEvent(event: ScimEvent) {
            _events.add(event)
        }

        fun clear() {
            _events.clear()
        }
    }

    private lateinit var client: ScimClient

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var eventCollector: ScimEventCollector

    @BeforeEach
    fun setup() {
        eventCollector.clear()
        client = ScimClientBuilder()
            .baseUrl("http://localhost:$port/scim/v2")
            .transport(HttpClientTransport())
            .serializer(JacksonScimSerializer())
            .build()
    }

    @Test
    fun `POST Users triggers ResourceCreatedEvent via Spring events`() {
        val user = User(userName = "event.create.${System.nanoTime()}")
        val response = client.create<User>("/Users", user)
        response.statusCode shouldBe 201

        eventCollector.events shouldHaveSize 1
        val event = eventCollector.events.first()
        event.shouldBeInstanceOf<ResourceCreatedEvent>()
        event.resourceType shouldBe "User"
        event.resourceId shouldBe response.value.id
    }

    @Test
    fun `PUT Users triggers ResourceReplacedEvent via Spring events`() {
        val user = User(userName = "event.replace.${System.nanoTime()}")
        val created = client.create<User>("/Users", user)
        val id = created.value.id!!

        eventCollector.clear()

        val updated = created.value.copy(displayName = "Updated")
        client.replace<User>("/Users", id, updated)

        eventCollector.events shouldHaveSize 1
        val event = eventCollector.events.first()
        event.shouldBeInstanceOf<ResourceReplacedEvent>()
        event.resourceType shouldBe "User"
        event.resourceId shouldBe id
    }

    @Test
    fun `full CRUD lifecycle produces correct event sequence`() {
        val user = User(userName = "event.lifecycle.${System.nanoTime()}")

        // Create
        val created = client.create<User>("/Users", user)
        val id = created.value.id!!

        // Replace
        val updated = created.value.copy(displayName = "Lifecycle User")
        client.replace<User>("/Users", id, updated)

        eventCollector.events shouldHaveSize 2
        eventCollector.events[0].shouldBeInstanceOf<ResourceCreatedEvent>()
        eventCollector.events[1].shouldBeInstanceOf<ResourceReplacedEvent>()

        eventCollector.events.forEach {
            it.resourceType shouldBe "User"
            it.resourceId shouldBe id
        }
    }
}
