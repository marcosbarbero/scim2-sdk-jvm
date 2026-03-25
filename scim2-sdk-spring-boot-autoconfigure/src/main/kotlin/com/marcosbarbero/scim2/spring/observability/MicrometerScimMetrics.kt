package com.marcosbarbero.scim2.spring.observability

import com.marcosbarbero.scim2.core.observability.ScimMetrics
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class MicrometerScimMetrics(private val registry: MeterRegistry) : ScimMetrics {

    private val activeGauges = mutableMapOf<String, AtomicLong>()

    override fun recordOperation(endpoint: String, method: String, status: Int, duration: Duration) {
        registry.timer(
            "scim.request.duration",
            "endpoint", endpoint,
            "method", method,
            "status", status.toString()
        ).record(duration)
    }

    override fun recordFilterParse(duration: Duration, success: Boolean) {
        registry.timer(
            "scim.filter.parse.duration",
            "success", success.toString()
        ).record(duration)
    }

    override fun recordPatchOperations(endpoint: String, operationCount: Int, duration: Duration) {
        registry.timer(
            "scim.patch.duration",
            "endpoint", endpoint
        ).record(duration)
        registry.counter(
            "scim.patch.operations",
            "endpoint", endpoint
        ).increment(operationCount.toDouble())
    }

    override fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: Duration) {
        registry.timer("scim.bulk.duration").record(duration)
        registry.counter("scim.bulk.operations").increment(operationCount.toDouble())
        registry.counter("scim.bulk.failures").increment(failureCount.toDouble())
    }

    override fun recordSearchResults(endpoint: String, totalResults: Int, duration: Duration) {
        registry.timer(
            "scim.search.duration",
            "endpoint", endpoint
        ).record(duration)
        registry.summary(
            "scim.search.results",
            "endpoint", endpoint
        ).record(totalResults.toDouble())
    }

    override fun incrementActiveRequests(endpoint: String) {
        getOrCreateGauge(endpoint).incrementAndGet()
    }

    override fun decrementActiveRequests(endpoint: String) {
        getOrCreateGauge(endpoint).decrementAndGet()
    }

    private fun getOrCreateGauge(endpoint: String): AtomicLong =
        activeGauges.computeIfAbsent(endpoint) { ep ->
            val gauge = AtomicLong(0)
            registry.gauge("scim.request.active", listOf(io.micrometer.core.instrument.Tag.of("endpoint", ep)), gauge) { it.toDouble() }
            gauge
        }
}
