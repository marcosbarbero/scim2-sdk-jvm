# ADR-0023: namastack-outbox Integration

## Status

Accepted

## Date

2026-03-25

## Context

The SCIM SDK publishes domain events (`ScimEvent`) through the `ScimEventPublisher` SPI. For production deployments, the transactional outbox pattern is the recommended approach to ensure reliable event delivery. The `ScimOutboxPort` interface was introduced to decouple the outbox storage mechanism from the SDK.

[namastack-outbox](https://github.com/namastack/namastack-outbox) is a transactional outbox library that integrates with Spring Modulith and supports multiple message brokers (Kafka, SQS, etc.). It provides exactly-once delivery semantics by storing events in the same database transaction as the business operation and relaying them asynchronously.

## Decision

We provide namastack-outbox integration as a **documented adapter pattern** rather than a compiled module dependency, because:

1. **namastack-outbox may not yet be in Maven Central** -- adding a hard compile dependency would prevent users from building the SDK.
2. **The adapter is minimal** -- it is a single class (~30 lines) that maps `ScimEvent` to namastack-outbox's `OutboxMessage`.
3. **Users may prefer other outbox implementations** (Debezium, custom JDBC, Spring Modulith events) and forcing a specific one violates the SDK's "no opinions on infrastructure" principle (see ADR-0001).

The adapter code is documented in `docs/outbox-pattern.md` under "Option 1: namastack-outbox". Once namastack-outbox reaches a stable Maven Central release, we may promote this to a dedicated `scim2-sdk-outbox-namastack` module.

### Mapping Strategy

| ScimEvent field     | OutboxMessage field |
|---------------------|---------------------|
| `resourceId`        | `aggregateId`       |
| `"ScimResource"`    | `aggregateType`     |
| class simple name   | `eventType`         |
| JSON serialization  | `payload`           |
| `correlationId`     | header              |
| `resourceType`      | header              |
| `eventId`           | header              |

## Consequences

- Users who want namastack-outbox integration copy the adapter from the docs into their project.
- No new compile-time dependency is introduced.
- When namastack-outbox is available in Maven Central, we can create a proper module with auto-configuration and a `@ConditionalOnClass` guard.
- The `ScimOutboxPort` SPI remains the stable contract; adapter implementations are interchangeable.
