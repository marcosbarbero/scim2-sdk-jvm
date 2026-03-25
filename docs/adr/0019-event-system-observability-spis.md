# ADR-0019: Event System and Observability SPIs

## Status

Accepted

## Context

The SCIM SDK needs to support observability (metrics, tracing, structured logging) and event-driven integration (publishing domain events after CRUD operations) without coupling to any specific observability framework (Micrometer, OpenTelemetry) or messaging system (Kafka, RabbitMQ, Spring Events).

Consumers need the ability to:
- Record operational metrics (request duration, active requests, error rates)
- Trace SCIM operations with correlation IDs
- React to domain events (resource created, replaced, patched, deleted, bulk completed)
- Use MDC-based structured logging for diagnostics

## Decision

We introduce three SPIs in `scim2-sdk-core`:

1. **`ScimEventPublisher`** - Publishes `ScimEvent` instances after successful CRUD and bulk operations. Events use a sealed class hierarchy (`ResourceCreatedEvent`, `ResourceReplacedEvent`, `ResourcePatchedEvent`, `ResourceDeletedEvent`, `BulkOperationCompletedEvent`) with correlation ID propagation.

2. **`ScimMetrics`** - Records operational metrics: request duration per endpoint/method/status, active request gauges, filter parse timing, patch operation counts, bulk operation counts, and search result counts.

3. **`ScimTracer`** - Wraps operations in trace spans with named operations and key-value attributes, and provides correlation ID resolution.

All three SPIs have `NoOp` default implementations (`NoOpEventPublisher`, `NoOpScimMetrics`, `NoOpScimTracer`) so the SDK works out of the box with zero observability overhead.

Additionally, a **`ScimOutboxPort`** in `scim2-sdk-server` provides a transactional outbox pattern for reliable event delivery.

The `ScimEndpointDispatcher` is instrumented to:
- Set SLF4J MDC keys (`scim.correlationId`, `scim.operation`, `scim.resourceType`) for structured logging
- Wrap request routing in tracer spans
- Record metrics for every dispatched request (duration, active request count)
- Publish domain events after successful create, replace, patch, delete, and bulk operations

## Consequences

- **Zero coupling**: No Micrometer, OpenTelemetry, or messaging dependency in core or server modules
- **Opt-in**: All SPIs default to no-op; consumers wire their preferred implementations
- **Testable**: `InMemoryEventPublisher` in `scim2-sdk-test` captures events for assertions
- **Extensible**: New event types can be added to the sealed hierarchy without breaking existing consumers (exhaustive `when` ensures compile-time safety)
- **MDC integration**: Structured logging works with any SLF4J-compatible logging framework
