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
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * End-to-end test validating that SCIM observability metrics are exposed
 * through Spring Boot Actuator endpoints after SCIM operations are performed.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "scim.base-url=http://scim.test",
        "management.endpoints.web.exposure.include=prometheus,metrics",
    ],
)
@Import(ScimObservabilityE2E.TestConfig::class)
class ScimObservabilityE2E(@LocalServerPort val port: Int) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun permitAllFilterChain(http: HttpSecurity): SecurityFilterChain = http
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
            .build()
    }

    private lateinit var client: ScimClient
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun setup() {
        client = ScimClientBuilder()
            .baseUrl("http://localhost:$port/scim/v2")
            .transport(HttpClientTransport())
            .serializer(JacksonScimSerializer())
            .build()
    }

    @Test
    fun `actuator metrics endpoint exposes scim request duration after SCIM request`() {
        // Perform a SCIM operation to generate metrics
        val user = User(userName = "observability.e2e.${System.nanoTime()}")
        val response = client.create<User>("/Users", user)
        response.statusCode shouldBe 201

        // Query the actuator metrics endpoint for scim.request.duration
        val metricsRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/actuator/metrics/scim.request.duration"))
            .GET()
            .build()
        val metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString())

        metricsResponse.statusCode() shouldBe 200
        val body = metricsResponse.body()
        body.shouldNotBeNull()

        // The metrics response should contain a measurement with COUNT > 0
        body shouldContain "\"name\":\"scim.request.duration\""
        body shouldContain "\"statistic\":\"COUNT\""

        // Parse the count value from the JSON response
        val objectMapper = JacksonScimSerializer.defaultObjectMapper()
        val json = objectMapper.readTree(body)
        val measurements = json["measurements"]
        measurements.shouldNotBeNull()

        val countMeasurement = measurements.firstOrNull { it["statistic"].stringValue() == "COUNT" }
        countMeasurement.shouldNotBeNull()
        countMeasurement["value"].doubleValue() shouldBeGreaterThan 0.0
    }

    @Test
    fun `actuator metrics endpoint exposes scim request active`() {
        // Perform a SCIM operation to trigger gauge registration
        val user = User(userName = "observability.active.${System.nanoTime()}")
        client.create<User>("/Users", user)

        // Query the actuator metrics endpoint for scim.request.active
        val metricsRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/actuator/metrics/scim.request.active"))
            .GET()
            .build()
        val metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString())

        metricsResponse.statusCode() shouldBe 200
        val body = metricsResponse.body()
        body.shouldNotBeNull()
        body shouldContain "\"name\":\"scim.request.active\""
    }
}
