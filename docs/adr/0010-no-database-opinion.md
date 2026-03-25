# ADR-0010: Generic Core with OOTB Persistence Defaults

## Status
Accepted (updated 2026-03-25, originally accepted as "No Database Opinion")

## Context
The SDK must not couple core/server modules to any specific database. However, requiring every user to implement `ResourceRepository<T>` from scratch creates unnecessary boilerplate for common setups. The spring-cloud-zuul-ratelimit project (https://github.com/marcosbarbero/spring-cloud-zuul-ratelimit) demonstrates an effective pattern: generic interfaces in the core with OOTB implementations that auto-configure based on classpath detection.

## Decision
- Core and server modules define `ResourceRepository<T>` with zero database dependencies
- The Spring Boot autoconfigure module provides OOTB default implementations:
  - **JPA** (`JpaResourceRepository`) — auto-detected via `@ConditionalOnClass(EntityManager.class)`
  - Additional backends (MongoDB, DynamoDB, etc.) can be added as separate adapter modules
- Each default implementation:
  - Is configurable via `scim.persistence.*` properties
  - Provides expected database schema via Flyway/Liquibase migration files
  - Backs off via `@ConditionalOnMissingBean` so users can replace with their own
- The test module provides `InMemoryResourceRepository` for testing

## Consequences
- Works OOTB for most users with JPA/Hibernate
- Fully replaceable for custom or niche database setups
- Core/server modules remain clean of database dependencies
- Each new backend is an additive change (new module or auto-config class)
