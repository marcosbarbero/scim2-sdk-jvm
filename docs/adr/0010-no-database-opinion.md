# ADR-0010: No Database Opinion

## Status

Accepted

## Context

SCIM service providers need to persist resources, but different organizations use different storage backends (PostgreSQL, MongoDB, DynamoDB, LDAP, in-memory). Coupling the SDK to a specific persistence technology would limit adoption.

## Decision

All persistence in the SDK is behind generic interfaces (e.g., `ResourceRepository<T>`). The SDK will not include any JPA, JDBC, or database-specific dependencies. Implementers provide their own persistence adapters.

## Consequences

### Positive

- SDK works with any storage backend
- No transitive database driver dependencies
- Clean separation between domain logic and persistence
- Aligns with hexagonal architecture (ADR-0008)

### Negative

- Implementers must write their own persistence layer
- Cannot provide out-of-the-box storage without additional modules
- Common patterns (pagination, filtering) must be well-documented for implementers
