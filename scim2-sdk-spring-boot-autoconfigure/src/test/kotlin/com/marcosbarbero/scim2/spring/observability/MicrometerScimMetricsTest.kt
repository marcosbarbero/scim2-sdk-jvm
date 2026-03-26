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
package com.marcosbarbero.scim2.spring.observability

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class MicrometerScimMetricsTest {

    private val faker = Faker()
    private lateinit var registry: SimpleMeterRegistry
    private lateinit var metrics: MicrometerScimMetrics

    @BeforeEach
    fun setUp() {
        registry = SimpleMeterRegistry()
        metrics = MicrometerScimMetrics(registry)
    }

    @Test
    fun `recordOperation records timer with endpoint, method and status tags`() {
        val endpoint = "/${faker.animal.name().replace(" ", "")}"
        val method = "GET"
        val status = 200
        val duration = Duration.ofMillis(faker.random.nextInt(10, 500).toLong())

        metrics.recordOperation(endpoint, method, status, duration)

        val timer = registry.find("scim.request.duration")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("status", status.toString())
            .timer()
        timer!!.count() shouldBe 1
        timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) shouldBeGreaterThan 0.0
    }

    @Test
    fun `recordFilterParse records timer with success tag`() {
        val duration = Duration.ofMillis(faker.random.nextInt(1, 100).toLong())

        metrics.recordFilterParse(duration, success = true)

        val timer = registry.find("scim.filter.parse.duration")
            .tag("success", "true")
            .timer()
        timer!!.count() shouldBe 1
    }

    @Test
    fun `recordFilterParse records timer with failure tag`() {
        val duration = Duration.ofMillis(faker.random.nextInt(1, 100).toLong())

        metrics.recordFilterParse(duration, success = false)

        val timer = registry.find("scim.filter.parse.duration")
            .tag("success", "false")
            .timer()
        timer!!.count() shouldBe 1
    }

    @Test
    fun `recordPatchOperations records timer and counter with endpoint tag`() {
        val endpoint = "/${faker.animal.name().replace(" ", "")}"
        val operationCount = faker.random.nextInt(1, 10)
        val duration = Duration.ofMillis(faker.random.nextInt(10, 200).toLong())

        metrics.recordPatchOperations(endpoint, operationCount, duration)

        val timer = registry.find("scim.patch.duration")
            .tag("endpoint", endpoint)
            .timer()
        timer!!.count() shouldBe 1

        val counter = registry.find("scim.patch.operations")
            .tag("endpoint", endpoint)
            .counter()
        counter!!.count() shouldBe operationCount.toDouble()
    }

    @Test
    fun `recordBulkOperation records timer and counters`() {
        val operationCount = faker.random.nextInt(5, 20)
        val failureCount = faker.random.nextInt(0, operationCount)
        val duration = Duration.ofMillis(faker.random.nextInt(50, 500).toLong())

        metrics.recordBulkOperation(operationCount, failureCount, duration)

        val timer = registry.find("scim.bulk.duration").timer()
        timer!!.count() shouldBe 1

        val opsCounter = registry.find("scim.bulk.operations").counter()
        opsCounter!!.count() shouldBe operationCount.toDouble()

        val failCounter = registry.find("scim.bulk.failures").counter()
        failCounter!!.count() shouldBe failureCount.toDouble()
    }

    @Test
    fun `recordSearchResults records timer and summary with endpoint tag`() {
        val endpoint = "/${faker.animal.name().replace(" ", "")}"
        val totalResults = faker.random.nextInt(0, 100)
        val duration = Duration.ofMillis(faker.random.nextInt(10, 300).toLong())

        metrics.recordSearchResults(endpoint, totalResults, duration)

        val timer = registry.find("scim.search.duration")
            .tag("endpoint", endpoint)
            .timer()
        timer!!.count() shouldBe 1

        val summary = registry.find("scim.search.results")
            .tag("endpoint", endpoint)
            .summary()
        summary!!.count() shouldBe 1
        summary.totalAmount() shouldBe totalResults.toDouble()
    }

    @Test
    fun `incrementActiveRequests increments gauge for endpoint`() {
        val endpoint = "/${faker.animal.name().replace(" ", "")}"

        metrics.incrementActiveRequests(endpoint)

        val gauge = registry.find("scim.request.active")
            .tag("endpoint", endpoint)
            .gauge()
        gauge!!.value() shouldBe 1.0
    }

    @Test
    fun `decrementActiveRequests decrements gauge for endpoint`() {
        val endpoint = "/${faker.animal.name().replace(" ", "")}"

        metrics.incrementActiveRequests(endpoint)
        metrics.incrementActiveRequests(endpoint)
        metrics.decrementActiveRequests(endpoint)

        val gauge = registry.find("scim.request.active")
            .tag("endpoint", endpoint)
            .gauge()
        gauge!!.value() shouldBe 1.0
    }

    @Test
    fun `active requests gauge returns zero after balanced inc and dec`() {
        val endpoint = "/${faker.animal.name().replace(" ", "")}"

        metrics.incrementActiveRequests(endpoint)
        metrics.decrementActiveRequests(endpoint)

        val gauge = registry.find("scim.request.active")
            .tag("endpoint", endpoint)
            .gauge()
        gauge!!.value() shouldBe 0.0
    }

    @Test
    fun `multiple endpoints have independent gauges`() {
        val endpoint1 = "/Users"
        val endpoint2 = "/Groups"

        metrics.incrementActiveRequests(endpoint1)
        metrics.incrementActiveRequests(endpoint1)
        metrics.incrementActiveRequests(endpoint2)

        val gauge1 = registry.find("scim.request.active")
            .tag("endpoint", endpoint1)
            .gauge()
        gauge1!!.value() shouldBe 2.0

        val gauge2 = registry.find("scim.request.active")
            .tag("endpoint", endpoint2)
            .gauge()
        gauge2!!.value() shouldBe 1.0
    }
}
