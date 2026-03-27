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
import com.marcosbarbero.scim2.client.api.search
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Real end-to-end test with Keycloak + suvera/keycloak-scim2-storage plugin.
 *
 * Flow:
 * 1. Keycloak (with SCIM extension) runs in Docker
 * 2. SCIM server (this Spring Boot app) runs on localhost
 * 3. Keycloak SCIM extension is configured to point to the SCIM server
 * 4. A user is created in Keycloak via Admin REST API
 * 5. The SCIM extension automatically pushes the user to our SCIM server
 * 6. The test verifies the user appears in the SCIM server
 *
 * Requires Docker. Automatically skipped when Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(KeycloakScimRealE2E.TestConfig::class)
class KeycloakScimRealE2E {

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

    companion object {
        private const val REALM = "scim-e2e"
        private const val KC_IMAGE = "suvera/keycloak-scim2-storage:v0.2"
        private const val ADMIN_USER = "admin"
        private const val ADMIN_PASS = "admin"

        private val objectMapper = ObjectMapper()
        private val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        private var keycloak: GenericContainer<*>? = null
        private var dockerAvailable = false

        @BeforeAll
        @JvmStatic
        fun checkDocker() {
            dockerAvailable = try {
                org.testcontainers.DockerClientFactory.instance().isDockerAvailable
            } catch (_: Exception) {
                false
            }
            assumeTrue(dockerAvailable, "Docker is not available -- skipping real Keycloak SCIM E2E")
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var eventCollector: ScimEventCollector

    private lateinit var scimClient: ScimClient

    @BeforeEach
    fun setup() {
        eventCollector.clear()

        if (keycloak == null || !keycloak!!.isRunning) {
            startKeycloak()
        }

        scimClient = ScimClientBuilder()
            .baseUrl("http://localhost:$port/scim/v2")
            .transport(HttpClientTransport())
            .serializer(JacksonScimSerializer())
            .build()
    }

    private fun startKeycloak() {
        // Expose the SCIM server port so Keycloak container can reach it
        Testcontainers.exposeHostPorts(port)

        keycloak = GenericContainer(KC_IMAGE)
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
            .withCommand("start-dev")
            .waitingFor(Wait.forHttp("/realms/master").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)))

        keycloak!!.start()

        val adminToken = obtainAdminToken()
        createRealm(adminToken)
        configureScimProvider(adminToken)
    }

    @Test
    fun `user created in Keycloak is automatically provisioned to SCIM server`() {
        val uniqueSuffix = System.nanoTime().toString()
        val username = "real.e2e.$uniqueSuffix"

        // Create user in Keycloak via Admin REST API
        val adminToken = obtainAdminToken()
        val keycloakUserId = createKeycloakUser(
            adminToken = adminToken,
            username = username,
            email = "$username@example.com",
            firstName = "Real",
            lastName = "E2E",
        )
        keycloakUserId.shouldNotBeBlank()

        // Wait for the SCIM extension's async job to push the user
        val userFound = pollForUser(username, timeout = Duration.ofSeconds(30))
        userFound shouldBe true

        // Verify the event was received through the Spring event bridge
        val createdEvents = eventCollector.events.filterIsInstance<ResourceCreatedEvent>()
        createdEvents.any { it.resourceType == "User" } shouldBe true
    }

    // --- Keycloak Admin helpers ---

    private fun keycloakUrl(): String = "http://${keycloak!!.host}:${keycloak!!.getMappedPort(8080)}"

    private fun obtainAdminToken(): String {
        val tokenUrl = "${keycloakUrl()}/realms/master/protocol/openid-connect/token"
        val formData = "grant_type=password&client_id=admin-cli&username=${URLEncoder.encode(ADMIN_USER, Charsets.UTF_8)}&password=${URLEncoder.encode(ADMIN_PASS, Charsets.UTF_8)}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formData))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "Failed to get admin token: ${response.statusCode()} ${response.body()}" }
        return objectMapper.readTree(response.body()).get("access_token").stringValue()
    }

    private fun createRealm(adminToken: String) {
        val realmJson = """{"realm":"$REALM","enabled":true}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${keycloakUrl()}/admin/realms"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $adminToken")
            .POST(HttpRequest.BodyPublishers.ofString(realmJson))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 201 || response.statusCode() == 409) {
            "Failed to create realm: ${response.statusCode()} ${response.body()}"
        }
    }

    private fun configureScimProvider(adminToken: String) {
        // Get realm ID
        val realmRequest = HttpRequest.newBuilder()
            .uri(URI.create("${keycloakUrl()}/admin/realms/$REALM"))
            .header("Authorization", "Bearer $adminToken")
            .GET()
            .build()
        val realmResponse = httpClient.send(realmRequest, HttpResponse.BodyHandlers.ofString())
        val realmId = objectMapper.readTree(realmResponse.body()).get("id").stringValue()

        // The SCIM server URL as seen from inside the Docker container
        val scimEndpoint = "http://host.testcontainers.internal:$port/scim/v2"

        val componentJson = objectMapper.writeValueAsString(
            mapOf(
                "name" to "SCIM E2E Provider",
                "providerId" to "skss-scim2-storage",
                "providerType" to "org.keycloak.storage.UserStorageProvider",
                "parentId" to realmId,
                "config" to mapOf(
                    "endPoint" to listOf(scimEndpoint),
                    "bearerToken" to emptyList<String>(),
                    "username" to emptyList<String>(),
                    "password" to emptyList<String>(),
                    "priority" to listOf("0"),
                    "enabled" to listOf("true"),
                ),
            ),
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("${keycloakUrl()}/admin/realms/$REALM/components"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $adminToken")
            .POST(HttpRequest.BodyPublishers.ofString(componentJson))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 201) {
            "Failed to configure SCIM provider: ${response.statusCode()} ${response.body()}"
        }
    }

    private fun createKeycloakUser(
        adminToken: String,
        username: String,
        email: String,
        firstName: String,
        lastName: String,
    ): String {
        val userJson = objectMapper.writeValueAsString(
            mapOf(
                "username" to username,
                "email" to email,
                "firstName" to firstName,
                "lastName" to lastName,
                "enabled" to true,
                "emailVerified" to true,
            ),
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${keycloakUrl()}/admin/realms/$REALM/users"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $adminToken")
            .POST(HttpRequest.BodyPublishers.ofString(userJson))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 201) {
            "Failed to create Keycloak user: ${response.statusCode()} ${response.body()}"
        }
        return response.headers().firstValue("Location").orElseThrow().substringAfterLast("/")
    }

    private fun pollForUser(username: String, timeout: Duration): Boolean {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (System.currentTimeMillis() < deadline) {
            try {
                val result = scimClient.search<User>("/Users", SearchRequest(filter = "userName eq \"$username\""))
                if (result.statusCode == 200 && result.value.totalResults > 0) {
                    return true
                }
            } catch (_: Exception) {
                // ignore and retry
            }
            Thread.sleep(1000)
        }
        return false
    }
}
