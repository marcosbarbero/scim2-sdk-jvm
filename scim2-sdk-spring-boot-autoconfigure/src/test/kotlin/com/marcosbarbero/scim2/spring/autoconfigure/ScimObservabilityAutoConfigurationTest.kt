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

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.core.observability.ScimTracer
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import com.marcosbarbero.scim2.spring.observability.MicrometerScimMetrics
import com.marcosbarbero.scim2.spring.observability.OpenTelemetryScimTracer
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimObservabilityAutoConfigurationTest {

    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ScimObservabilityAutoConfiguration::class.java,
            ),
        )

    private val tracerContextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ScimTracerAutoConfiguration::class.java,
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
                context.getBeansOfType(ScimMetrics::class.java).isEmpty() shouldBe true
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

    @Test
    fun `SCIM request through dispatcher records metrics in MeterRegistry`() {
        val fullContextRunner = ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    ScimJacksonAutoConfiguration::class.java,
                    ScimServerAutoConfiguration::class.java,
                    ScimObservabilityAutoConfiguration::class.java,
                ),
            )

        fullContextRunner
            .withPropertyValues("scim.base-url=http://test.local")
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .withBean("testUserHandler", ResourceHandler::class.java, { StubUserHandler() })
            .run { context ->
                val dispatcher = context.getBean(ScimEndpointDispatcher::class.java)
                val registry = context.getBean(MeterRegistry::class.java)

                val serializer = JacksonScimSerializer()
                val userJson = serializer.serialize(User(userName = "metrics.test"))

                // Dispatch a POST /Users request
                val request = ScimHttpRequest(
                    method = HttpMethod.POST,
                    path = "/scim/v2/Users",
                    headers = mapOf("Content-Type" to listOf("application/scim+json")),
                    body = userJson,
                )
                val response = dispatcher.dispatch(request)
                response.status shouldBe 201

                // Verify scim.request.duration timer was recorded
                val timer = registry.find("scim.request.duration")
                    .tag("endpoint", "/Users")
                    .tag("method", "POST")
                    .tag("status", "201")
                    .timer()
                timer.shouldNotBeNull()
                timer.count() shouldBe 1

                // Verify scim.request.active gauge exists (should be 0 after request completes)
                val gauge = registry.find("scim.request.active")
                    .tag("endpoint", "/Users")
                    .gauge()
                gauge.shouldNotBeNull()
                gauge.value() shouldBe 0.0
            }
    }

    @Test
    fun `creates OpenTelemetryScimTracer when Tracer bean present`() {
        val tracer = otelTesting.openTelemetry.getTracer("test")
        tracerContextRunner
            .withBean(Tracer::class.java, { tracer })
            .run { context ->
                val scimTracer = context.getBean(ScimTracer::class.java)
                scimTracer.shouldNotBeNull()
                scimTracer.shouldBeInstanceOf<OpenTelemetryScimTracer>()
            }
    }

    @Test
    fun `does not create tracer when scim tracing enabled is false`() {
        val tracer = otelTesting.openTelemetry.getTracer("test")
        tracerContextRunner
            .withPropertyValues("scim.tracing.enabled=false")
            .withBean(Tracer::class.java, { tracer })
            .run { context ->
                context.getBeansOfType(ScimTracer::class.java).isEmpty() shouldBe true
            }
    }

    private class StubUserHandler : ResourceHandler<User> {
        override val resourceType: Class<User> = User::class.java
        override val endpoint: String = "/Users"

        override fun get(id: String, context: ScimRequestContext): User = User(id = id, userName = "stub")

        override fun create(resource: User, context: ScimRequestContext): User = resource.copy(id = "generated-id")

        override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User = resource.copy(id = id)

        override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User = User(id = id, userName = "patched")

        override fun delete(id: String, version: String?, context: ScimRequestContext) {}

        override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> = ListResponse(totalResults = 0, resources = emptyList())
    }
}
