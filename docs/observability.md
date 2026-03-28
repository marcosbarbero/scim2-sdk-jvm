# Observability Guide

The SCIM 2.0 SDK provides built-in observability through three pillars:

- **Metrics** -- Micrometer-based instrumentation for all SCIM operations
- **Structured Logging** -- MDC-enriched log entries with correlation IDs
- **Event Correlation** -- Every SCIM event carries a tracer-provided correlation ID

These integrate with standard observability stacks (Prometheus, Grafana, ELK, Datadog, etc.) with zero custom code when using Spring Boot.

## Getting Started

### Spring Boot (recommended)

Add the starter and actuator dependencies:

```xml
<dependency>
    <groupId>com.marcosbarbero</groupId>
    <artifactId>scim2-sdk-spring-boot-starter</artifactId>
    <version>${scim2-sdk.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- For Prometheus export -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

That is all. When Micrometer's `MeterRegistry` is on the classpath, the auto-configuration registers a `MicrometerScimMetrics` bean that records all metrics listed below.

### Without Spring Boot

Pass a `ScimMetrics` implementation (or `NoOpScimMetrics`) when constructing the `ScimEndpointDispatcher`:

```kotlin
val metrics: ScimMetrics = MicrometerScimMetrics(yourMeterRegistry)

val dispatcher = ScimEndpointDispatcher(
    handlers = handlers,
    serializer = serializer,
    config = config,
    metrics = metrics,
    tracer = NoOpScimTracer,
    // ...
)
```

## Available Metrics

| Metric Name | Type | Tags | Description |
|---|---|---|---|
| `scim.request.duration` | Timer | `endpoint`, `method`, `status` | Total duration of a SCIM HTTP request. Includes the full lifecycle: interceptors, handler execution, serialization, and meta-enrichment. |
| `scim.request.active` | Gauge | `endpoint` | Number of SCIM requests currently in flight for a given endpoint. |
| `scim.search.duration` | Timer | `endpoint` | Duration of the handler's search execution only (excludes request deserialization, pagination defaults, response serialization, and meta enrichment). |
| `scim.search.results` | Summary | `endpoint` | Distribution of result-set sizes returned by search operations. |
| `scim.patch.duration` | Timer | `endpoint` | Duration of the handler's patch execution only (excludes request deserialization, ETag handling, response serialization, and meta enrichment). |
| `scim.patch.operations` | Counter | `endpoint` | Cumulative count of individual PATCH operations processed (a single PATCH request may contain multiple operations). |
| `scim.bulk.duration` | Timer | *(none)* | Total duration of a bulk request. |
| `scim.bulk.operations` | Counter | *(none)* | Cumulative count of individual operations within bulk requests. |
| `scim.bulk.failures` | Counter | *(none)* | Cumulative count of failed operations within bulk requests. |
| `scim.filter.parse.duration` | Timer | `success` | Duration of SCIM filter expression parsing. **Note:** This metric is not currently instrumented by the dispatcher because filter parsing occurs inside `FilterEngine` (core module), which does not depend on `ScimMetrics`. Custom repository implementations that parse filters directly can call `ScimMetrics.recordFilterParse()` to record parse timings. |

### Timing Semantics

There are two categories of duration metrics:

- **Full-request timers** (`scim.request.duration`) measure the complete HTTP request lifecycle, from the moment the dispatcher receives the request to the moment the response is ready. This includes interceptor execution, handler invocation, JSON serialization/deserialization, ETag handling, and `meta` enrichment.

- **Handler-only timers** (`scim.search.duration`, `scim.patch.duration`, `scim.bulk.duration`) measure only the time spent inside the handler's business logic. They exclude framework overhead like deserialization, pagination defaults, response serialization, and meta enrichment.

This distinction lets you separate framework overhead from your handler's actual processing time. If `scim.request.duration` is high but `scim.search.duration` is low, the bottleneck is in serialization or interceptors, not in your persistence layer.

## Structured Logging

The `ScimEndpointDispatcher` automatically sets and clears SLF4J MDC keys for every SCIM request:

| MDC Key | Description | Example Value |
|---|---|---|
| `scim.correlationId` | Correlation ID from the `ScimTracer` (or empty if no tracer is active) | `a3f1b2c4-...` |
| `scim.operation` | HTTP method and relative path | `GET /Users/abc123` |
| `scim.resourceType` | SCIM resource type derived from the request path | `Users` |

These keys are available to any logging framework that supports MDC (Logback, Log4j2, etc.). Configure your log pattern to include them:

```xml
<!-- Logback example -->
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} [%X{scim.correlationId}] [%X{scim.operation}] - %msg%n</pattern>
```

MDC keys are set before handler execution and cleaned up in a `finally` block, so they are always removed even if the handler throws an exception.

## Event Correlation

Every `ScimEvent` (e.g., `ResourceCreatedEvent`, `ResourcePatchedEvent`, `BulkOperationCompletedEvent`) carries an optional `correlationId` field. When a `ScimTracer` is active, the dispatcher calls `tracer.currentCorrelationId()` and passes it to the event. This allows you to correlate events with the request that triggered them across distributed systems.

The sealed event hierarchy:

- `ResourceCreatedEvent` -- emitted after a successful CREATE
- `ResourceReplacedEvent` -- emitted after a successful PUT (replace)
- `ResourcePatchedEvent` -- emitted after a successful PATCH
- `ResourceDeletedEvent` -- emitted after a successful DELETE
- `BulkOperationCompletedEvent` -- emitted after a bulk request completes

All events include `correlationId: String?` inherited from `ScimEvent`.

## Spring Boot Configuration

A complete observability configuration for `application.yml`:

```yaml
scim:
  base-path: /scim/v2
  base-url: http://localhost:8080

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,metrics
  metrics:
    tags:
      application: my-scim-server     # common tag applied to all metrics
    distribution:
      percentiles-histogram:
        scim.request.duration: true    # enables histogram buckets for percentile queries
        scim.search.duration: true

logging:
  level:
    com.marcosbarbero.scim2: DEBUG     # SDK debug logging
```

Key settings:

- `management.endpoints.web.exposure.include` -- expose the Prometheus scrape endpoint at `/actuator/prometheus`
- `management.metrics.distribution.percentiles-histogram` -- enable histogram buckets so Prometheus can compute p50/p95/p99 latencies
- `management.metrics.tags.application` -- add a common `application` tag to all metrics for multi-service dashboards

## Prometheus + Grafana Setup

The [Spring Boot full-stack sample](../scim2-sdk-samples/sample-fullstack-spring/) includes a complete Prometheus + Grafana stack:

```
scim2-sdk-samples/sample-fullstack-spring/
  docker/
    prometheus/prometheus.yml          # scrape config targeting the SCIM server
    grafana/
      provisioning/
        datasources/prometheus.yml     # auto-provisions Prometheus as a data source
        dashboards/dashboard.yml       # auto-provisions the SCIM dashboard
      dashboards/
        scim-overview.json             # pre-built Grafana dashboard
```

To run the full stack:

```bash
cd scim2-sdk-samples/sample-fullstack-spring
docker compose up -d
```

This starts:
- The SCIM server on port 8080 (with `/actuator/prometheus` endpoint)
- Prometheus on port 9090 (scraping the SCIM server)
- Grafana on port 3000 (with the pre-built SCIM dashboard)

The pre-built Grafana dashboard (`scim-overview.json`) includes panels for request rates, latency percentiles, active requests, search result distributions, bulk operation counts, and error rates.

## Custom Implementations

### Custom ScimMetrics

To provide your own metrics implementation, implement the `ScimMetrics` interface:

```kotlin
class DatadogScimMetrics(private val statsd: StatsDClient) : ScimMetrics {
    override fun recordOperation(endpoint: String, method: String, status: Int, duration: Duration) {
        statsd.recordExecutionTime("scim.request.duration", duration.toMillis(),
            "endpoint:$endpoint", "method:$method", "status:$status")
    }

    override fun recordFilterParse(duration: Duration, success: Boolean) { /* ... */ }
    override fun recordPatchOperations(endpoint: String, operationCount: Int, duration: Duration) { /* ... */ }
    override fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: Duration) { /* ... */ }
    override fun recordSearchResults(endpoint: String, totalResults: Int, duration: Duration) { /* ... */ }
    override fun incrementActiveRequests(endpoint: String) { /* ... */ }
    override fun decrementActiveRequests(endpoint: String) { /* ... */ }
}
```

With Spring Boot, register it as a bean and the auto-configuration will use it instead of `MicrometerScimMetrics` (thanks to `@ConditionalOnMissingBean`):

```kotlin
@Bean
fun scimMetrics(): ScimMetrics = DatadogScimMetrics(statsdClient)
```

### Custom ScimTracer

To provide your own tracer (e.g., for OpenTelemetry spans), implement `ScimTracer`:

```kotlin
class OpenTelemetryScimTracer(private val tracer: Tracer) : ScimTracer {
    override fun <T> trace(operationName: String, attributes: Map<String, String>, block: () -> T): T {
        val span = tracer.spanBuilder(operationName).startSpan()
        attributes.forEach { (k, v) -> span.setAttribute(k, v) }
        return span.makeCurrent().use {
            try {
                block()
            } finally {
                span.end()
            }
        }
    }

    override fun currentCorrelationId(): String? =
        Span.current().spanContext.traceId.takeIf { it != TraceId.getInvalid() }
}
```

Register it as a Spring bean or pass it directly to `ScimEndpointDispatcher`.

### Disabling Metrics

If you want to explicitly disable metrics without removing Micrometer from the classpath, provide the no-op implementation:

```kotlin
@Bean
fun scimMetrics(): ScimMetrics = NoOpScimMetrics
```

Or without Spring:

```kotlin
val dispatcher = ScimEndpointDispatcher(
    // ...
    metrics = NoOpScimMetrics,
    tracer = NoOpScimTracer,
)
```
