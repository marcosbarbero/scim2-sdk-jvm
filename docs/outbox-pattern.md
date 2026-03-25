# Outbox Pattern for Reliable Event Publishing

## Why This Matters for SCIM

SCIM provisioning is inherently event-driven: when an Identity Provider like Azure AD, Okta, or Keycloak provisions users to your application, downstream systems often need to react — sending welcome emails, syncing to LDAP, updating access control lists, notifying audit systems, triggering workflows.

### The Problem: Dual-Write Inconsistency

Without the outbox pattern, a typical implementation does this:

```mermaid
sequenceDiagram
    participant IdP as Identity Provider
    participant App as Your SCIM Server
    participant DB as Database
    participant MQ as Message Broker<br/>(Kafka, RabbitMQ)

    IdP->>App: POST /scim/v2/Users
    App->>DB: INSERT user ✓
    App->>MQ: Publish "UserCreated" event ✗ (broker down!)
    App-->>IdP: 201 Created

    Note over DB,MQ: User exists in DB but<br/>downstream systems never<br/>learned about it
```

This is the **dual-write problem**: your application writes to two systems (database + message broker) without transactional guarantees across them. If the message broker is unavailable — even briefly — events are silently lost. Downstream systems fall out of sync.

In SCIM provisioning, this is especially dangerous because:
- **Azure AD bulk syncs** can push 100,000+ user changes in minutes during initial provisioning or org mergers
- **Okta real-time provisioning** sends individual SCIM operations for every HR change
- **Lost events mean real-world impact**: a new hire doesn't get their email, a terminated employee retains access, group memberships are inconsistent

### The Solution: Transactional Outbox

The outbox pattern eliminates the dual-write by storing events in the **same database transaction** as the resource change:

```mermaid
sequenceDiagram
    participant IdP as Identity Provider
    participant App as Your SCIM Server
    participant DB as Database
    participant Poller as Outbox Poller<br/>(async)
    participant MQ as Message Broker

    IdP->>App: POST /scim/v2/Users

    rect rgb(200, 230, 200)
        Note over App,DB: Single Database Transaction
        App->>DB: INSERT user
        App->>DB: INSERT outbox event
        Note over DB: COMMIT (atomic)
    end

    App-->>IdP: 201 Created

    Note over Poller,MQ: Asynchronous, retryable
    Poller->>DB: SELECT unprocessed events
    Poller->>MQ: Publish events
    Poller->>DB: Mark processed
```

**Guarantees:**
- **Atomicity**: The event is stored in the same transaction as the resource change. If the DB write fails, no event is created. If the event insert fails, the resource write rolls back.
- **Reliability**: Events are never lost — they persist in the database until successfully published, even across application restarts or broker outages.
- **Ordering**: Events are processed in order (by created_at timestamp).
- **Idempotency**: Each event has a unique `eventId` — downstream consumers can deduplicate.
- **Backpressure**: The poller publishes at its own pace. A flood of SCIM operations doesn't overwhelm the message broker.

### When You DON'T Need the Outbox

The outbox pattern adds complexity. You don't need it if:
- Your application has no downstream event consumers
- Fire-and-forget events are acceptable (use Spring `ApplicationEvent` instead — see Option 3 below)
- Your SCIM server is a simple CRUD app with no async workflows
- You're in early development and can accept occasional event loss

The SDK defaults to **no outbox** (`NoOpEventPublisher`). You opt in only when you need reliable event publishing.

### How It Relates to Spring Modulith

[Spring Modulith](https://spring.io/projects/spring-modulith) promotes event-driven communication between application modules. The outbox pattern aligns directly:

- `ScimEvent` instances can be published as Spring `ApplicationEvent`s
- Spring Modulith's [Event Externalization](https://docs.spring.io/spring-modulith/reference/events.html#externalization) can forward them to Kafka, AMQP, etc.
- [namastack-outbox](https://github.com/namastack/namastack-outbox) provides the transactional outbox infrastructure that Spring Modulith uses

```mermaid
graph LR
    SCIM[SCIM Operation] --> EP[ScimEventPublisher]
    EP --> SE[Spring ApplicationEvent]
    SE --> SM[Spring Modulith<br/>Event Externalization]
    SM --> K[Kafka]
    SM --> A[AMQP]
    SM --> O[Other]

    EP --> OP[ScimOutboxPort]
    OP --> NO[namastack-outbox<br/>Transactional Store]
    NO --> SM

    style SCIM fill:#4CAF50,color:#fff
    style NO fill:#2196F3,color:#fff
    style SM fill:#9C27B0,color:#fff
```

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

[namastack-outbox](https://github.com/namastack/namastack-outbox) provides transactional outbox support and is available in Maven Central. It integrates with Spring Modulith's event externalization, meaning events stored via the outbox can be automatically forwarded to Kafka, AMQP, or any other supported broker through Spring Modulith's `EventExternalizationConfiguration`.

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

Implement `ScimOutboxPort` and register as a Spring bean.

**Kotlin:**

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

**Java:**

```java
@Component
public class MyOutboxPort implements ScimOutboxPort {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MyOutboxPort(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void store(ScimEvent event) {
        jdbcTemplate.update(
            "INSERT INTO my_outbox (event_id, event_type, payload, created_at) VALUES (?, ?, ?, ?)",
            event.getEventId(),
            event.getClass().getSimpleName(),
            objectMapper.writeValueAsString(event),
            event.getTimestamp()
        );
    }
}
```

### Option 3: Spring Application Events (No Outbox)

By default, the SDK publishes `ScimEvent` instances via `ScimEventPublisher`. In Spring, these become `ApplicationEvent` instances that you can listen to.

**Important:** This approach is **fire-and-forget** with **no delivery guarantees**. If your listener throws an exception, the event is lost. If the application restarts before the listener runs, the event is lost. Use this only when occasional event loss is acceptable.

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

## Real-World Example: Azure AD Provisioning with Outbox

```mermaid
sequenceDiagram
    participant AAD as Azure AD
    participant App as Your Application<br/>(SCIM + Outbox)
    participant DB as PostgreSQL
    participant K as Kafka
    participant Email as Email Service
    participant LDAP as LDAP Sync
    participant Audit as Audit Log

    Note over AAD,App: Initial bulk sync (50,000 users)
    loop For each user
        AAD->>App: POST /scim/v2/Users
        App->>DB: INSERT user + outbox event (single TX)
        App-->>AAD: 201 Created
    end

    Note over K,Audit: Async processing (at its own pace)
    loop Outbox poller (every 100ms)
        DB->>K: Batch of UserCreated events
        K->>Email: Send welcome emails
        K->>LDAP: Sync to corporate directory
        K->>Audit: Log provisioning action
    end

    Note over AAD,Audit: Even if Kafka was down during sync,<br/>events are safely in the outbox table<br/>and will be published when Kafka recovers
```
