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
package com.marcosbarbero.scim2.core.observability

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration

class ScimObservabilityTest {

    @Test
    fun `NoOpScimMetrics should not throw on any method`() {
        val d = Duration.ofMillis(100)
        NoOpScimMetrics.recordOperation("/Users", "GET", 200, d)
        NoOpScimMetrics.recordFilterParse(d, true)
        NoOpScimMetrics.recordFilterParse(d, false)
        NoOpScimMetrics.recordPatchOperations("/Users", 3, d)
        NoOpScimMetrics.recordBulkOperation(5, 1, d)
        NoOpScimMetrics.recordSearchResults("/Users", 10, d)
        NoOpScimMetrics.incrementActiveRequests("/Users")
        NoOpScimMetrics.decrementActiveRequests("/Users")
    }

    @Test
    fun `NoOpScimTracer should not throw on any method`() {
        NoOpScimTracer.currentCorrelationId() shouldBe null
    }

    @Test
    fun `NoOpScimTracer trace should execute block and return result`() {
        val result = NoOpScimTracer.trace("test.op") { 42 }
        result shouldBe 42
    }

    @Test
    fun `NoOpScimTracer trace should execute block with attributes`() {
        val result = NoOpScimTracer.trace("test.op", mapOf("key" to "value")) { "hello" }
        result shouldBe "hello"
    }

    @Test
    fun `ScimTracer trace should propagate exceptions from block`() {
        try {
            NoOpScimTracer.trace("test.op") { throw IllegalStateException("boom") }
        } catch (e: IllegalStateException) {
            e.message shouldBe "boom"
        }
    }
}
