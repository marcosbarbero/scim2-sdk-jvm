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
