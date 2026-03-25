# ADR-0003: Three-Tier Module Structure

## Status

Accepted

## Context

The SDK needs to serve different use cases: some consumers only need the SCIM schema model, others need a server-side handler framework, and others need a client to talk to SCIM providers. Bundling everything into a single artifact would force unnecessary dependencies on consumers.

## Decision

We will organize the SDK into three tiers:

1. **Core** (`scim2-sdk-core`) — Domain model, schema parsing, serialization SPI. Zero framework dependencies.
2. **Server** (`scim2-sdk-server`) — Handler framework for implementing SCIM service providers. Depends on core.
3. **Client** (`scim2-sdk-client`) — Fluent API and DSLs for SCIM consumers. Depends on core. Transport adapters (`scim2-sdk-client-httpclient`, `scim2-sdk-client-okhttp`) are separate modules.

Additional modules provide Spring Boot integration (`scim2-sdk-spring-boot-starter`) and test utilities (`scim2-sdk-test`).

## Consequences

### Positive

- Clean separation of concerns — consumers pull only what they need
- Core module has zero framework dependencies, maximizing reuse
- Transport adapters are pluggable — no classpath conflicts
- BOM module (`scim2-sdk-bom`) simplifies version management for consumers

### Negative

- More modules to maintain and release
- Inter-module dependency graph must be carefully managed
- Contributors need to understand which module owns which responsibility
