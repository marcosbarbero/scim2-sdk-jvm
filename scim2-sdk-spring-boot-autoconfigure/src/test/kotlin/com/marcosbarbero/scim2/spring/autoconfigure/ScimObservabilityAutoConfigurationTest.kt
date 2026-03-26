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
package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.spring.observability.MicrometerScimMetrics
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimObservabilityAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ScimObservabilityAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `creates MicrometerScimMetrics when MeterRegistry is present`() {
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .run { context ->
                val metrics = context.getBean(ScimMetrics::class.java)
                metrics.shouldNotBeNull()
                metrics.shouldBeInstanceOf<MicrometerScimMetrics>()
            }
    }

    @Test
    fun `does not create ScimMetrics when no MeterRegistry`() {
        contextRunner
            .run { context ->
                context.containsBean("micrometerScimMetrics") shouldBe false
            }
    }

    @Test
    fun `backs off when custom ScimMetrics provided`() {
        val customMetrics = object : ScimMetrics {
            override fun recordOperation(endpoint: String, method: String, status: Int, duration: java.time.Duration) {}
            override fun recordFilterParse(duration: java.time.Duration, success: Boolean) {}
            override fun recordPatchOperations(endpoint: String, operationCount: Int, duration: java.time.Duration) {}
            override fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: java.time.Duration) {}
            override fun recordSearchResults(endpoint: String, totalResults: Int, duration: java.time.Duration) {}
            override fun incrementActiveRequests(endpoint: String) {}
            override fun decrementActiveRequests(endpoint: String) {}
        }
        contextRunner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withBean("customMetrics", ScimMetrics::class.java, { customMetrics })
            .run { context ->
                context.containsBean("micrometerScimMetrics") shouldBe false
            }
    }
}
