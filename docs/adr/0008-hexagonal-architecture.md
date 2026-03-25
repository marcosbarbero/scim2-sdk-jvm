# ADR-0008: Hexagonal Architecture

## Status

Accepted

## Context

We need a clean separation between the SCIM domain logic (schema validation, resource manipulation, filtering) and infrastructure concerns (HTTP, serialization, persistence). The domain should be testable in isolation and must not depend on any framework.

## Decision

We will follow the hexagonal architecture (ports and adapters) pattern:

- **Domain** — Pure business logic, value objects, entities. No framework dependencies.
- **Ports** — Interfaces defining how the domain interacts with the outside world (e.g., `ResourceRepository`, `ScimSerializer`, `HttpTransport`).
- **Adapters** — Concrete implementations of ports (e.g., Jackson serializer, OkHttp transport, Spring MVC controller adapter).

Dependencies flow inward: adapters depend on ports, ports depend on domain. The domain never depends on adapters.

## Consequences

### Positive

- Domain logic is fully testable without frameworks or infrastructure
- Framework-agnostic core — can integrate with Spring, Micronaut, Quarkus, or plain Kotlin
- Clean boundaries make it easy to swap implementations (e.g., different serializer, different HTTP transport)
- Aligns naturally with the three-tier module structure (ADR-0003)

### Negative

- More interfaces and abstractions than a layered architecture
- Developers must understand the ports-and-adapters pattern
- Risk of over-engineering if boundaries are drawn too granularly
