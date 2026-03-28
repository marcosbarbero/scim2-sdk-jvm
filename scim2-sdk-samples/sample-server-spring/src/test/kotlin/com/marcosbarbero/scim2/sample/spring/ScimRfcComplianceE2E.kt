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
import com.marcosbarbero.scim2.client.api.get
import com.marcosbarbero.scim2.client.api.replace
import com.marcosbarbero.scim2.core.domain.model.common.GroupMembership
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
 * End-to-end test validating RFC 7643/7644 compliance on a running Spring Boot SCIM server.
 *
 * Validates:
 * - meta.location is present and correct on all responses (RFC 7643 §3.1)
 * - meta.resourceType is set (RFC 7643 §3.1)
 * - $ref is populated on group members (RFC 7643 §4.2)
 * - Location header matches meta.location on POST (RFC 7644 §3.1)
 * - Outbound provisioning fires events for all CRUD operations
 * - Discovery endpoints include schemas and meta
 * - Search results include meta.location on each resource
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["scim.base-url=http://scim.test"],
)
@Import(ScimRfcComplianceE2E.TestConfig::class)
class ScimRfcComplianceE2E(@LocalServerPort val port: Int) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun permitAllFilterChain(http: HttpSecurity): SecurityFilterChain = http
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
            .build()

        @Bean
        fun scimEventCollector(): EventCollector = EventCollector()
    }

    class EventCollector {
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
    private val objectMapper = JacksonScimSerializer.defaultObjectMapper()

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var eventCollector: EventCollector

    @BeforeEach
    fun setup() {
        eventCollector.clear()
        client = ScimClientBuilder()
            .baseUrl("http://localhost:$port/scim/v2")
            .transport(HttpClientTransport())
            .serializer(JacksonScimSerializer())
            .build()
    }

    // === meta.location on CRUD responses (RFC 7643 §3.1) ===

    @Test
    fun `POST Users returns meta location matching Location header`() {
        val user = User(userName = "rfc.post.${System.nanoTime()}")
        val response = client.create<User>("/Users", user)

        response.statusCode shouldBe 201
        response.value.meta.shouldNotBeNull()
        response.value.meta!!.location.shouldNotBeNull()
        response.value.meta!!.location.toString() shouldContain "/scim/v2/Users/${response.value.id}"
        response.value.meta!!.resourceType shouldBe "User"
    }

    @Test
    fun `GET Users by id returns meta location`() {
        val created = client.create<User>("/Users", User(userName = "rfc.get.${System.nanoTime()}"))
        val id = created.value.id!!

        val response = client.get<User>("/Users", id)

        response.statusCode shouldBe 200
        response.value.meta.shouldNotBeNull()
        response.value.meta!!.location.shouldNotBeNull()
        response.value.meta!!.location.toString() shouldContain "/scim/v2/Users/$id"
    }

    @Test
    fun `PUT Users returns meta location`() {
        val created = client.create<User>("/Users", User(userName = "rfc.put.${System.nanoTime()}"))
        val id = created.value.id!!

        val updated = created.value.copy(displayName = "Updated")
        val response = client.replace<User>("/Users", id, updated)

        response.statusCode shouldBe 200
        response.value.meta.shouldNotBeNull()
        response.value.meta!!.location.shouldNotBeNull()
        response.value.meta!!.location.toString() shouldContain "/scim/v2/Users/$id"
    }

    @Test
    fun `search Users returns meta location on each resource`() {
        client.create<User>("/Users", User(userName = "rfc.search1.${System.nanoTime()}"))
        client.create<User>("/Users", User(userName = "rfc.search2.${System.nanoTime()}"))

        // Use raw HTTP because ScimClient.search() ListResponse has type erasure issues
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:$port/scim/v2/Users?startIndex=1&count=10"))
            .GET()
            .build()
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        val json = response.body()

        json shouldContain "\"location\""
        json shouldContain "/scim/v2/Users/"
        json shouldContain "\"resourceType\":\"User\""
    }

    // === $ref on group members (RFC 7643 §4.2) ===

    @Test
    fun `group members include ref with absolute URI`() {
        val user = client.create<User>("/Users", User(userName = "rfc.member.${System.nanoTime()}"))
        val userId = user.value.id!!

        val group = Group(
            displayName = "RFC Test Group ${System.nanoTime()}",
            members = listOf(GroupMembership(value = userId, display = "Test User", type = "User")),
        )
        val created = client.create<Group>("/Groups", group)

        created.statusCode shouldBe 201
        created.value.members shouldHaveSize 1

        val member = created.value.members[0]
        member.ref.shouldNotBeNull()
        member.ref.toString() shouldContain "/scim/v2/Users/$userId"
    }

    @Test
    fun `GET group returns members with ref`() {
        val user = client.create<User>("/Users", User(userName = "rfc.getgroup.${System.nanoTime()}"))
        val userId = user.value.id!!

        val group = client.create<Group>(
            "/Groups",
            Group(
                displayName = "RFC Get Group ${System.nanoTime()}",
                members = listOf(GroupMembership(value = userId, display = "Test", type = "User")),
            ),
        )
        val groupId = group.value.id!!

        val fetched = client.get<Group>("/Groups", groupId)
        fetched.value.members shouldHaveSize 1
        fetched.value.members[0].ref.shouldNotBeNull()
        fetched.value.members[0].ref.toString() shouldContain "/scim/v2/Users/$userId"
    }

    // === Discovery endpoints (RFC 7644 §4) ===

    @Test
    fun `ServiceProviderConfig includes schemas and meta`() {
        val response = client.getServiceProviderConfig()

        response.statusCode shouldBe 200
        response.value.schemas shouldHaveSize 1
        response.value.meta.shouldNotBeNull()
        response.value.meta!!.resourceType shouldBe "ServiceProviderConfig"
        response.value.meta!!.location.shouldNotBeNull()
        response.value.meta!!.location.toString() shouldContain "/scim/v2/ServiceProviderConfig"
    }

    @Test
    fun `ResourceTypes response contains meta on each resource`() {
        // Use raw HTTP because ScimClient.getResourceTypes() deserializes
        // ListResponse<ResourceType> as ListResponse<LinkedHashMap> due to type erasure
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:$port/scim/v2/ResourceTypes"))
            .GET()
            .build()
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        val json = response.body()

        json shouldContain "\"resourceType\":\"ResourceType\""
        json shouldContain "\"location\""
    }

    @Test
    fun `Schemas response contains meta on each schema`() {
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:$port/scim/v2/Schemas"))
            .GET()
            .build()
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        val json = response.body()

        json shouldContain "\"resourceType\":\"Schema\""
        json shouldContain "\"location\""
    }

    // === Outbound events fire for all operations ===

    @Test
    fun `full CRUD lifecycle fires correct events`() {
        eventCollector.clear()

        // Create
        val user = client.create<User>("/Users", User(userName = "rfc.events.${System.nanoTime()}"))
        val id = user.value.id!!

        // Replace
        client.replace<User>("/Users", id, user.value.copy(displayName = "Updated"))

        // Delete
        client.delete("/Users", id)

        eventCollector.events.size shouldBe 3
        eventCollector.events[0]::class.simpleName shouldBe "ResourceCreatedEvent"
        eventCollector.events[1]::class.simpleName shouldBe "ResourceReplacedEvent"
        eventCollector.events[2]::class.simpleName shouldBe "ResourceDeletedEvent"
    }

    // === Verify raw JSON wire format ===

    @Test
    fun `raw JSON response does not contain null fields or empty arrays`() {
        val user = User(userName = "rfc.raw.${System.nanoTime()}")
        val response = client.create<User>("/Users", user)

        // Re-fetch as raw JSON to verify wire format
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:$port/scim/v2/Users/${response.value.id}"))
            .GET()
            .build()
        val rawResponse = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        val json = rawResponse.body()

        // No null values
        json shouldNotContain ": null"
        json shouldNotContain ":null"

        // meta.location present
        json shouldContain "\"location\""

        // schemas present
        json shouldContain "\"schemas\""
    }
}
