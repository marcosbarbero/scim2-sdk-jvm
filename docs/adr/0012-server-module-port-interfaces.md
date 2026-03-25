# ADR-0012: Server Module Port Interfaces and Endpoint Dispatcher

## Status

Accepted

## Context

The SCIM 2.0 SDK needs a server-side module that handles HTTP routing, request dispatching, and protocol-level concerns (bulk, pagination, ETags, discovery) without coupling to any specific web framework, database, or identity provider. Per ADR-0008 (hexagonal architecture), dependencies must flow inward, and all external concerns must be behind port interfaces.

## Decision

We introduce `scim2-sdk-server` with the following architecture:

**Port Interfaces** (`server/port/`):
- `ResourceHandler<T>` — CRUD + search operations per SCIM resource type
- `ResourceRepository<T>` — Generic persistence port (no DB opinion, per ADR-0010)
- `BulkHandler` — Optional bulk processing delegation
- `MeHandler` — Optional /Me endpoint support
- `IdentityResolver` — Resolves caller identity from HTTP request
- `AuthorizationEvaluator` — Fine-grained authorization with permissive defaults

**HTTP Abstractions** (`server/http/`):
- `ScimHttpRequest` / `ScimHttpResponse` — Framework-agnostic HTTP model
- `HttpMethod` enum

**Adapter Layer** (`server/adapter/`):
- `ScimEndpointDispatcher` — Full routing engine for all SCIM endpoints
- `DiscoveryService` — ServiceProviderConfig, Schemas, ResourceTypes

**Engines** (`server/engine/`):
- `ETagEngine` — Weak ETag generation and precondition checking
- `PaginationEngine` — In-memory pagination producing `ListResponse`
- `BulkEngine` — Self-contained bulk operation processor with cross-reference resolution

**SPI** (`server/interceptor/`):
- `ScimInterceptor` — Ordered pre/post handle and error hooks

**Configuration** (`server/config/`):
- `ScimServerConfig` — All protocol knobs (bulk limits, filter max, pagination defaults)

## Consequences

- Framework adapters (Spring MVC, Ktor, etc.) only need to bridge `ScimHttpRequest`/`ScimHttpResponse` to their native types and delegate to `ScimEndpointDispatcher`.
- No runtime dependency on any web framework, database, or identity provider.
- `ResourceHandler` is the primary integration point for implementors.
- `ScimInterceptor` provides a clean extension point for cross-cutting concerns (logging, metrics, tenant isolation) without modifying the dispatcher.
- Engines are self-contained and independently testable.
