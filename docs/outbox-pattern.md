# Outbox Pattern for Reliable Event Publishing

## Overview

SCIM provisioning at scale (e.g., Azure AD syncing 100K+ users) can overwhelm downstream systems. The outbox pattern ensures reliable event publishing by storing events in the same database transaction as the resource change.

## How It Works

```mermaid
sequenceDiagram
    participant Client
    participant SCIM Server
    participant DB
    participant Outbox Poller
    participant Message Broker

    Client->>SCIM Server: POST /Users
    SCIM Server->>DB: BEGIN TX
    SCIM Server->>DB: INSERT user
    SCIM Server->>DB: INSERT outbox event
    SCIM Server->>DB: COMMIT TX
    SCIM Server-->>Client: 201 Created

    loop Periodic poll
        Outbox Poller->>DB: SELECT unprocessed events
        Outbox Poller->>Message Broker: Publish events
        Outbox Poller->>DB: Mark events processed
    end
```

## Using the ScimOutboxPort

The `ScimOutboxPort` interface in the server module defines the outbox contract:

```kotlin
interface ScimOutboxPort {
    fun store(event: ScimEvent)
}
```

### Option 1: namastack-outbox (Recommended)

[namastack-outbox](https://github.com/namastack/namastack-outbox) provides transactional outbox support and is available in Maven Central. It integrates with Spring Modulith's event externalization.

Add the dependency:

```xml
<dependency>
    <groupId>com.namastack</groupId>
    <artifactId>namastack-outbox-spring-boot-starter</artifactId>
    <version>${namastack-outbox.version}</version>
</dependency>
```

Configure in `application.yml`:

```yaml
scim:
  outbox:
    enabled: true
```

The SDK auto-configures a `NamastackOutboxAdapter` implementing `ScimOutboxPort` that delegates to namastack-outbox's event publishing. Events are stored transactionally alongside your resource changes and published asynchronously.

### Option 2: Custom Implementation

Implement `ScimOutboxPort` and register as a Spring bean:

```kotlin
@Component
class MyOutboxPort(private val jdbcTemplate: JdbcTemplate) : ScimOutboxPort {
    override fun store(event: ScimEvent) {
        jdbcTemplate.update(
            "INSERT INTO my_outbox (event_id, event_type, payload, created_at) VALUES (?, ?, ?, ?)",
            event.eventId, event::class.simpleName, objectMapper.writeValueAsString(event), event.timestamp
        )
    }
}
```

### Option 3: Spring Application Events (No Outbox)

By default, the SDK publishes `ScimEvent` instances via `ScimEventPublisher`. In Spring, these become `ApplicationEvent` instances that you can listen to:

```kotlin
@Component
class ScimEventListener {
    @EventListener
    fun onResourceCreated(event: ResourceCreatedEvent) {
        // Handle event (non-transactional, fire-and-forget)
    }
}
```

## Database Schema for Outbox

If using a custom outbox implementation, here is a reference schema:

```sql
CREATE TABLE scim_outbox (
    event_id        VARCHAR(255) NOT NULL PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    resource_id     VARCHAR(255) NOT NULL,
    correlation_id  VARCHAR(255),
    payload         TEXT NOT NULL,
    processed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP
);

CREATE INDEX idx_scim_outbox_unprocessed ON scim_outbox (processed, created_at) WHERE processed = FALSE;
```
