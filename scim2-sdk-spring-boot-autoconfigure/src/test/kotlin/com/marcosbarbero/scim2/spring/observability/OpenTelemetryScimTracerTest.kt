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

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension

class OpenTelemetryScimTracerTest {

    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    private val tracer = OpenTelemetryScimTracer(otelTesting.openTelemetry.getTracer("scim-test"))

    @Test
    fun `trace wraps block in span and returns result`() {
        val result = tracer.trace("scim.create", mapOf("endpoint" to "/Users")) { "created" }

        result shouldBe "created"

        val spans = otelTesting.spans
        spans shouldHaveSize 1
        spans[0].name shouldBe "scim.create"
        spans[0].attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("endpoint")) shouldBe "/Users"
    }

    @Test
    fun `trace sets attributes on span`() {
        tracer.trace("scim.search", mapOf("endpoint" to "/Groups", "filter" to "displayName eq \"Admins\"")) { }

        val spans = otelTesting.spans
        spans shouldHaveSize 1
        spans[0].attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("endpoint")) shouldBe "/Groups"
        spans[0].attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("filter")) shouldBe "displayName eq \"Admins\""
    }

    @Test
    fun `trace records exception on failure`() {
        assertThrows<IllegalStateException> {
            tracer.trace("scim.delete", emptyMap()) {
                throw IllegalStateException("not found")
            }
        }

        val spans = otelTesting.spans
        spans shouldHaveSize 1
        spans[0].status.statusCode shouldBe StatusCode.ERROR
        spans[0].status.description shouldBe "not found"
        spans[0].events shouldHaveSize 1
        spans[0].events[0].name shouldBe "exception"
    }

    @Test
    fun `currentCorrelationId returns traceId when span is active`() {
        var correlationId: String? = null
        tracer.trace("scim.get", emptyMap()) {
            correlationId = tracer.currentCorrelationId()
        }

        correlationId.shouldNotBeNull()
        correlationId!!.shouldNotBeBlank()
    }

    @Test
    fun `currentCorrelationId returns null when no span is active`() {
        // Outside of any span, Span.current() returns an invalid span
        val correlationId = tracer.currentCorrelationId()
        correlationId.shouldBeNull()
    }

    @Test
    fun `span is active during block execution`() {
        var spanDuringExecution: Span? = null
        tracer.trace("scim.patch", mapOf("method" to "PATCH")) {
            spanDuringExecution = Span.current()
        }

        val captured = spanDuringExecution
        captured.shouldNotBeNull()
        captured.spanContext.isValid shouldBe true
    }
}
